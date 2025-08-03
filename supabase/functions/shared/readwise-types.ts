// Readwise API Types

export interface ReadwiseDocument {
  id: string;
  url: string;
  title: string;
  author?: string;
  category: 'article' | 'email' | 'rss' | 'pdf' | 'epub' | 'tweet' | 'video';
  location: 'new' | 'later' | 'shortlist' | 'archive';
  tags: Record<string, string>;
  created_at: string;
  updated_at: string;
  notes: string;
  published_date?: string;
  summary?: string;
  image_url?: string;
  content?: string;
  html_content?: string;
  reading_progress: number;
  first_opened_at?: string;
  last_opened_at?: string;
  saved_at: string;
  last_moved_at: string;
}

export interface ReadwiseListResponse {
  count: number;
  nextPageCursor?: string;
  results: ReadwiseDocument[];
}

export interface ReadwiseListParams {
  id?: string;
  updatedAfter?: string;
  location?: 'new' | 'later' | 'shortlist' | 'archive';
  category?: 'article' | 'email' | 'rss' | 'pdf' | 'epub' | 'tweet' | 'video';
  tag?: string;
  pageCursor?: string;
  withHtmlContent?: boolean;
}

export interface ReadwiseClient {
  listDocuments(params?: ReadwiseListParams): Promise<ReadwiseListResponse>;
}