export interface Prompt {
  promptUuid?: string;
  userId?: string;
  name: string;
  description: string;
  promptText: string;
  outputFormat?: 'text_only' | 'schema' | 'schema_with_text';
  responseColumnName?: string;
  includeFullResponse?: boolean;
  outputSchema: string;
  createTimestamp?: Date;
  updateTimestamp?: Date;
}