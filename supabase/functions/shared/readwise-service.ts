// Readwise Service with Convenience Methods

import type { ReadwiseClient, ReadwiseDocument, ReadwiseListParams, ReadwiseListResponse } from './readwise-types.ts';

export class ReadwiseService {
  constructor(private client: ReadwiseClient) {}

  async getDocumentById(id: string, withHtmlContent = false): Promise<ReadwiseDocument | null> {
    const response = await this.client.listDocuments({ id, withHtmlContent });
    return response.results[0] || null;
  }

  async getDocumentsUpdatedAfter(date: string, params: Omit<ReadwiseListParams, 'updatedAfter'> = {}): Promise<ReadwiseListResponse> {
    return this.client.listDocuments({ ...params, updatedAfter: date });
  }

  async getDocumentsAfterCursor(cursor: string, params: Omit<ReadwiseListParams, 'pageCursor'> = {}): Promise<ReadwiseListResponse> {
    return this.client.listDocuments({ ...params, pageCursor: cursor });
  }

  async getAllDocuments(params: Omit<ReadwiseListParams, 'pageCursor'> = {}): Promise<ReadwiseDocument[]> {
    const allDocuments: ReadwiseDocument[] = [];
    let nextPageCursor: string | undefined;

    do {
      const response = await this.client.listDocuments({ ...params, pageCursor: nextPageCursor });
      allDocuments.push(...response.results);
      nextPageCursor = response.nextPageCursor;
    } while (nextPageCursor);

    return allDocuments;
  }

}

export function createReadwiseService(client: ReadwiseClient): ReadwiseService {
  return new ReadwiseService(client);
}