import { AgentContext, AgentResult } from './agent-types';
import { BaseAgent } from './base.agent';
import axios from 'axios';
import { downloadMediaMessage } from 'baileys';
import { PDFParse } from 'pdf-parse';
import mammoth from 'mammoth';
import P from 'pino';

export class DocumentAgent extends BaseAgent {
  constructor() {
    super({
      type: 'document',
      name: 'Document Agent',
      capabilities: ['pdf-extraction', 'docx-extraction', 'txt-extraction'],
    });
  }

  canHandle(context: AgentContext): boolean {
    const content = context.content || '';
    return content.includes('documentMessage') || content.includes('documentWithCaption');
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const extractedText = await this.extractDocumentText(context.msg, context.instance);

      if (!extractedText) {
        return this.failure('Failed to extract document text');
      }

      const docMsg =
        context.msg.message?.documentMessage ||
        context.msg.message?.documentWithCaptionMessage?.message?.documentMessage;
      const fileName = docMsg?.fileName || 'document';

      const enrichedContent = `[document: ${fileName}]\n${extractedText}`;

      return this.success(enrichedContent, undefined, {
        fileName,
        extractedLength: extractedText.length,
        processingTime: Date.now() - startTime,
      });
    } catch (error: any) {
      return this.failure(error.message || 'Document agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }

  private async extractDocumentText(msg: any, instance: any): Promise<string | null> {
    try {
      const docMsg =
        msg.message?.documentMessage ||
        msg.message?.documentWithCaptionMessage?.message?.documentMessage;

      if (!docMsg) {
        this.logger.warn('[DocumentAgent] Document message object not found');
        return null;
      }

      const mimetype: string = docMsg.mimetype || '';
      const fileName: string = docMsg.fileName || 'document';
      const fileSize: number = docMsg.fileLength || 0;

      if (fileSize > 10 * 1024 * 1024) {
        this.logger.warn(`[DocumentAgent] Document too large: ${fileSize} bytes (${fileName})`);
        return null;
      }

      let fileBuffer: Buffer;

      if (msg.message.mediaUrl) {
        const response = await axios.get(msg.message.mediaUrl, {
          responseType: 'arraybuffer',
          timeout: 30000,
        });
        fileBuffer = Buffer.from(response.data, 'binary');
      } else if (msg.message.base64) {
        fileBuffer = Buffer.from(msg.message.base64, 'base64');
      } else {
        fileBuffer = await downloadMediaMessage(
          { key: msg.key, message: msg.message },
          'buffer',
          {},
          {
            logger: P({ level: 'error' }) as any,
            reuploadRequest: instance,
          },
        );
      }

      this.logger.log(`[DocumentAgent] Downloaded: ${fileBuffer.length} bytes, mimetype: ${mimetype}, fileName: ${fileName}`);

      let extractedText = '';

      if (mimetype === 'application/pdf' || fileName.toLowerCase().endsWith('.pdf')) {
        const parser = new PDFParse({ data: fileBuffer });
        const pdfData = await parser.getText();
        extractedText = pdfData.text;
        this.logger.log(`[DocumentAgent] PDF extracted: ${extractedText.length} chars`);
      } else if (
        mimetype === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
        fileName.toLowerCase().endsWith('.docx')
      ) {
        const result = await mammoth.extractRawText({ buffer: fileBuffer });
        extractedText = result.value;
        this.logger.log(`[DocumentAgent] DOCX extracted: ${extractedText.length} chars`);
      } else if (mimetype === 'text/plain' || fileName.toLowerCase().endsWith('.txt')) {
        extractedText = fileBuffer.toString('utf-8');
        this.logger.log(`[DocumentAgent] TXT extracted: ${extractedText.length} chars`);
      } else {
        this.logger.warn(`[DocumentAgent] Unsupported type: ${mimetype} (${fileName})`);
        return null;
      }

      const keepPercent = parseFloat(process.env.DOCUMENT_KEEP_PERCENT || '0.9');
      const hardCap = parseInt(process.env.DOCUMENT_MAX_CHARS || '200000', 10);
      const keepChars = Math.min(Math.floor(extractedText.length * keepPercent), hardCap);
      if (extractedText.length > keepChars) {
        const originalLen = extractedText.length;
        extractedText = extractedText.substring(0, keepChars) + '\n[... documento truncado]';
        this.logger.log(`[DocumentAgent] Truncated to ${keepChars} chars (${Math.round(keepPercent * 100)}% of ${originalLen})`);
      }

      const meaningfulText = extractedText.replace(/--\s*\d+\s+of\s+\d+\s*--/g, '').replace(/\s+/g, '').trim();
      if (!extractedText.trim() || meaningfulText.length < 20) {
        this.logger.warn(`[DocumentAgent] No meaningful text extracted from ${fileName} (${meaningfulText.length} chars of real content) — likely image-based PDF`);
        return null;
      }

      return extractedText;
    } catch (error: any) {
      this.logger.error(`[DocumentAgent] Error extracting: ${error.message}`);
      return null;
    }
  }
}
