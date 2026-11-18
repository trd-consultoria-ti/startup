import { BaseChatbotDto, BaseChatbotSettingDto } from '../../base-chatbot.dto';

export class AnthropicCredsDto {
  name: string;
  apiKey: string;
}

export class AnthropicDto extends BaseChatbotDto {
  anthropicCredsId: string;
  model?: string;
  systemMessages?: string[];
  assistantMessages?: string[];
  userMessages?: string[];
  maxTokens?: number;
}

export class AnthropicSettingDto extends BaseChatbotSettingDto {
  anthropicCredsId?: string;
  anthropicIdFallback?: string;
  speechToText?: boolean;
}

export class CreateReminderDto {
  number?: string;
  remoteJid?: string;
  message: string;
  triggerAt?: string;
  recurrence?: string;
  botId: string;
}
