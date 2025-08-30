export interface ValidationMessageSummary {
  message: string;
  count: number;
}

export interface Job {
  jobUuid: string;
  userId: string;
  fileUuid: string;
  fileName: string;
  promptUuid: string;
  promptName: string;
  modelId: string;
  status: string;
  taskCount: number;
  resultFileUuid?: string;
  completedTaskCount: number;
  createdAt: string;
  updatedAt: string;
  maxTokens?: number;
  temperature?: number;
  maxRecords?: number;
  startRecordNumber?: number;
  creditUsage?: number;
  costEstimate?: number;
  creditEstimate?: number;
  errorMessage?: string;
  validationMessageSummary?: ValidationMessageSummary[];
}
