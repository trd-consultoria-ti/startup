import { Logger } from '@config/logger.config';
import { spawn } from 'child_process';
import { OLLAMA_CONFIG } from './agent-types';

export interface ClaudeCLIResult {
  response: string;
  sessionId: string;
}

export class ClaudeCLI {
  private readonly logger = new Logger('ClaudeCLI');
  private readonly maxConcurrent = 3;
  private activeCount = 0;
  private readonly waitQueue: (() => void)[] = [];

  get config() {
    return OLLAMA_CONFIG;
  }

  private async acquireSlot(): Promise<void> {
    if (this.activeCount < this.maxConcurrent) {
      this.activeCount++;
      return;
    }
    await new Promise<void>((resolve) => {
      this.waitQueue.push(resolve);
    });
    this.activeCount++;
  }

  private releaseSlot(): void {
    this.activeCount--;
    const next = this.waitQueue.shift();
    if (next) next();
  }

  async executeText(
    prompt: string,
    systemPrompt: string,
    model: string,
    sessionId?: string,
  ): Promise<ClaudeCLIResult> {
    await this.acquireSlot();
    try {
      return await this.executeTextInner(prompt, systemPrompt, model, sessionId);
    } catch (err: any) {
      if (sessionId) {
        this.logger.warn(`[ClaudeCLI] Text --resume failed (${err.message}), retrying without resume...`);
        return await this.executeTextInner(prompt, systemPrompt, model, '');
      }
      throw err;
    } finally {
      this.releaseSlot();
    }
  }

  private executeTextInner(
    prompt: string,
    systemPrompt: string,
    model: string,
    sessionId?: string,
  ): Promise<ClaudeCLIResult> {
    return new Promise((resolve, reject) => {
      const args = [
        '-p',
        '--model', model,
        '--system-prompt', systemPrompt,
        '--tools', '',
        '--output-format', 'json',
        '--allow-dangerously-skip-permissions',
      ];

      if (sessionId) {
        args.push('--resume', sessionId);
        this.logger.log(`[ClaudeCLI] Resuming session: ${sessionId}`);
      }

      this.logger.log(`[ClaudeCLI] spawn claude --model ${model} --output-format json${sessionId ? ' --resume ' + sessionId : ''}`);

      const isWindows = process.platform === 'win32';
      const spawnOpts: any = {
        stdio: ['pipe', 'pipe', 'pipe'],
        env: {
          ...process.env,
          ANTHROPIC_API_KEY: OLLAMA_CONFIG.apiKey,
          ANTHROPIC_BASE_URL: OLLAMA_CONFIG.baseUrl,
        },
        timeout: OLLAMA_CONFIG.timeout,
      };
      if (!isWindows) {
        spawnOpts.env.HOME = '/home/claudeuser';
        spawnOpts.uid = 1001;
        spawnOpts.gid = 1001;
      }

      const child = spawn('claude', args, spawnOpts);

      let stdout = '';
      let stderr = '';
      let timedOut = false;

      const timer = setTimeout(() => {
        timedOut = true;
        child.kill('SIGTERM');
      }, OLLAMA_CONFIG.timeout);

      child.stdout.on('data', (chunk) => (stdout += chunk));
      child.stderr.on('data', (chunk) => (stderr += chunk));

      child.on('error', (err) => {
        clearTimeout(timer);
        this.logger.error(`[ClaudeCLI] Spawn error: ${err.message}`);
        reject(new Error(`Claude CLI spawn failed: ${err.message}`));
      });

      child.on('close', (code) => {
        clearTimeout(timer);
        this.logger.log(`[ClaudeCLI] exited | code=${code} | timedOut=${timedOut} | stdoutLen=${stdout.length}`);
        if (timedOut || code !== 0) {
          if (stderr) this.logger.error(`[ClaudeCLI] stderr: ${stderr.trim().substring(0, 500)}`);
          reject(new Error(`Claude CLI failed (exit ${code})`));
          return;
        }

        const rawOutput = stdout.toString().trim();
        if (!rawOutput) {
          if (stderr) this.logger.warn(`[ClaudeCLI] stderr: ${stderr.trim().substring(0, 500)}`);
          reject(new Error('Claude CLI returned empty response'));
          return;
        }

        let response = '';
        let sid = '';
        try {
          const parsed = JSON.parse(rawOutput);
          response = parsed.result || parsed.text || '';
          sid = parsed.session_id || '';
        } catch {
          this.logger.warn(`[ClaudeCLI] Failed to parse JSON, using raw output`);
          response = rawOutput;
        }

        response = response.trim();
        if (!response) {
          reject(new Error('Claude CLI returned no text content'));
          return;
        }

        resolve({ response, sessionId: sid });
      });

      child.stdin.on('error', (err) => {
        this.logger.warn(`[ClaudeCLI] stdin error (non-fatal): ${err.message}`);
      });

      child.stdin.write(prompt);
      child.stdin.end();
    });
  }

  async executeImage(
    prompt: string,
    systemPrompt: string,
    model: string,
    imageBase64: string,
    imageMimeType: string,
    sessionId?: string,
  ): Promise<ClaudeCLIResult> {
    await this.acquireSlot();
    try {
      return await this.executeImageInner(prompt, systemPrompt, model, imageBase64, imageMimeType, sessionId);
    } catch (err: any) {
      if (sessionId) {
        this.logger.warn(`[ClaudeCLI] Image --resume failed (${err.message}), retrying without resume...`);
        return await this.executeImageInner(prompt, systemPrompt, model, imageBase64, imageMimeType, '');
      }
      throw err;
    } finally {
      this.releaseSlot();
    }
  }

  private executeImageInner(
    prompt: string,
    systemPrompt: string,
    model: string,
    imageBase64: string,
    imageMimeType: string,
    sessionId?: string,
  ): Promise<ClaudeCLIResult> {
    return new Promise((resolve, reject) => {
      const args = [
        '-p',
        '--model', model,
        '--system-prompt', systemPrompt,
        '--tools', '',
        '--output-format', 'stream-json',
        '--input-format', 'stream-json',
        '--verbose',
        '--allow-dangerously-skip-permissions',
      ];

      if (sessionId) {
        args.push('--resume', sessionId);
      }

      this.logger.log(`[ClaudeCLI] spawn claude (image) --model ${model} | base64=${imageBase64.length} chars`);

      const isWindowsImg = process.platform === 'win32';
      const spawnOptsImg: any = {
        stdio: ['pipe', 'pipe', 'pipe'],
        env: {
          ...process.env,
          ANTHROPIC_API_KEY: OLLAMA_CONFIG.apiKey,
          ANTHROPIC_BASE_URL: OLLAMA_CONFIG.baseUrl,
        },
        timeout: OLLAMA_CONFIG.timeout,
      };
      if (!isWindowsImg) {
        spawnOptsImg.env.HOME = '/home/claudeuser';
        spawnOptsImg.uid = 1001;
        spawnOptsImg.gid = 1001;
      }

      const child = spawn('claude', args, spawnOptsImg);

      let stdout = '';
      let stderr = '';
      let timedOut = false;

      const timer = setTimeout(() => {
        timedOut = true;
        child.kill('SIGTERM');
      }, OLLAMA_CONFIG.timeout);

      child.stdout.on('data', (chunk) => (stdout += chunk));
      child.stderr.on('data', (chunk) => (stderr += chunk));

      child.on('error', (err) => {
        clearTimeout(timer);
        reject(new Error(`Claude CLI spawn failed: ${err.message}`));
      });

      child.on('close', (code) => {
        clearTimeout(timer);
        if (timedOut || code !== 0) {
          if (stderr) this.logger.error(`[ClaudeCLI] stderr: ${stderr.trim().substring(0, 500)}`);
          reject(new Error(`Claude CLI failed (exit ${code})`));
          return;
        }

        const rawOutput = stdout.toString().trim();
        if (!rawOutput) {
          reject(new Error('Claude CLI returned empty response'));
          return;
        }

        let response = '';
        let sid = '';
        try {
          const lines = rawOutput.split('\n').filter((l) => l.trim());
          for (const line of lines) {
            const parsed = JSON.parse(line);
            if (parsed.type === 'result' && parsed.result) {
              response = parsed.result;
              if (parsed.session_id) sid = parsed.session_id;
              break;
            }
            if (parsed.type === 'assistant' && parsed.message?.content) {
              for (const block of parsed.message.content) {
                if (block.type === 'text' && block.text) {
                  response += block.text;
                }
              }
            }
          }
        } catch {
          this.logger.warn(`[ClaudeCLI] Failed to parse stream-json, using raw output`);
          response = rawOutput;
        }

        response = response.trim();
        if (!response) {
          reject(new Error('Claude CLI returned no text content'));
          return;
        }

        resolve({ response, sessionId: sid });
      });

      child.stdin.on('error', (err) => {
        this.logger.warn(`[ClaudeCLI] stdin error (non-fatal): ${err.message}`);
      });

      const streamMessage = {
        type: 'user',
        message: {
          role: 'user',
          content: [
            { type: 'text', text: prompt },
            { type: 'image', source: { type: 'base64', media_type: imageMimeType, data: imageBase64 } },
          ],
        },
      };

      child.stdin.write(JSON.stringify(streamMessage));
      child.stdin.end();
    });
  }
}

export const claudeCLI = new ClaudeCLI();
