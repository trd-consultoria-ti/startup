import { ConfigService, Language } from '@config/env.config';
import { AgentContext, AgentResult, OLLAMA_CONFIG } from './agent-types';
import { BaseAgent } from './base.agent';
import axios from 'axios';
import { downloadMediaMessage } from 'baileys';
import FormData from 'form-data';
import ffmpeg from 'fluent-ffmpeg';
import ffmpegPath from '@ffmpeg-installer/ffmpeg';
import { PassThrough } from 'stream';
import P from 'pino';

export class AudioAgent extends BaseAgent {
  constructor(private readonly configService?: ConfigService) {
    super({
      type: 'audio',
      name: 'Audio Agent',
      capabilities: ['speech-to-text', 'transcription', 'whisper'],
    });
  }

  canHandle(context: AgentContext): boolean {
    return context.content.startsWith('audioMessage|');
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const transcription = await this.speechToText(context.msg, context.instance);

      if (!transcription) {
        return this.failure('Failed to transcribe audio');
      }

      return this.success(transcription, undefined, {
        processingTime: Date.now() - startTime,
        transcription: true,
      });
    } catch (error: any) {
      return this.failure(error.message || 'Audio agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }

  private async speechToText(msg: any, instance: any): Promise<string | null> {
    let audio: Buffer;

    if (msg.message.mediaUrl) {
      audio = await axios.get(msg.message.mediaUrl, { responseType: 'arraybuffer' }).then((r) =>
        Buffer.from(r.data, 'binary'),
      );
    } else if (msg.message.base64) {
      audio = Buffer.from(msg.message.base64, 'base64');
    } else {
      audio = await downloadMediaMessage(
        { key: msg.key, message: msg?.message },
        'buffer',
        {},
        {
          logger: P({ level: 'error' }) as any,
          reuploadRequest: instance,
        },
      );
    }

    const lang = this.configService?.get<Language>('LANGUAGE')?.includes('pt') ? 'pt' : 'en';

    this.logger.log(`[AudioAgent] Audio received: ${audio.length} bytes, lang: ${lang}`);

    const wavBuffer = await this.convertOggToWav(audio);
    this.logger.log(`[AudioAgent] Converted to WAV: ${wavBuffer.length} bytes`);

    const formData = new FormData();
    formData.append('file', wavBuffer, { filename: 'audio.wav', contentType: 'audio/wav' });
    formData.append('model', 'whisper-1');
    formData.append('language', lang);
    formData.append('response_format', 'json');

    const whisperUrl = OLLAMA_CONFIG.whisperUrl;
    this.logger.log(`[AudioAgent] Sending to whisper at ${whisperUrl}/inference`);

    try {
      const response = await axios.post(`${whisperUrl}/inference`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000,
      });

      const text = response?.data?.text || response?.data?.transcription;
      if (text) {
        this.logger.log(`[AudioAgent] Transcription OK: "${text.substring(0, 100)}"`);
        return text;
      }

      this.logger.warn('[AudioAgent] whisper returned empty response');
      return '[audio transcription unavailable]';
    } catch (err: any) {
      this.logger.warn(`[AudioAgent] whisper failed: ${err.message}`);
      return '[audio transcription unavailable]';
    }
  }

  private async convertOggToWav(oggBuffer: Buffer): Promise<Buffer> {
    return new Promise((resolve, reject) => {
      ffmpeg.setFfmpegPath(ffmpegPath.path);
      const inputStream = new PassThrough();
      inputStream.end(oggBuffer);
      const outputStream = new PassThrough();
      const chunks: Buffer[] = [];

      outputStream.on('data', (chunk) => chunks.push(chunk));
      outputStream.on('end', () => resolve(Buffer.concat(chunks)));
      outputStream.on('error', (err) => reject(err));

      ffmpeg(inputStream)
        .inputFormat('ogg')
        .outputFormat('wav')
        .audioCodec('pcm_s16le')
        .audioFrequency(16000)
        .audioChannels(1)
        .pipe(outputStream, { end: true })
        .on('error', (err) => reject(err));
    });
  }
}
