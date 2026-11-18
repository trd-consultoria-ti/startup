import { AnthropicRouter } from '@api/integrations/chatbot/anthropic/routes/anthropic.router';
import { ChatwootRouter } from '@api/integrations/chatbot/chatwoot/routes/chatwoot.router';
import { Router } from 'express';

export class ChatbotRouter {
  public readonly router: Router;

  constructor(...guards: any[]) {
    this.router = Router();

    this.router.use('/chatwoot', new ChatwootRouter(...guards).router);
    this.router.use('/anthropic', new AnthropicRouter(...guards).router);
  }
}
