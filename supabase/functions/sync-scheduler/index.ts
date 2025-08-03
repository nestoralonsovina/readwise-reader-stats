// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { createReadwiseClient } from "../shared/readwise-base-client.ts"
import { createReadwiseService } from "../shared/readwise-service.ts"

Deno.serve(async (req) => {
  try {
    const accessToken = Deno.env.get('READWISE_ACCESS_TOKEN');
    if (!accessToken) {
      throw new Error('READWISE_ACCESS_TOKEN environment variable is required');
    }

    const client = createReadwiseClient(accessToken);
    const readwiseService = createReadwiseService(client);
    
    // Example: Get recent documents updated in the last 24 hours
    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
    const recentDocuments = await readwiseService.getDocumentsUpdatedAfter(yesterday);

    console.log(`Found ${recentDocuments.count} documents updated in the last 24 hours`);

    const data = {
      message: "Sync completed successfully",
      documentsFound: recentDocuments.count,
      documents: recentDocuments.results.slice(0, 5) // Return first 5 for preview
    };

    return new Response(
      JSON.stringify(data),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (error) {
    console.error('Sync error:', error);
    return new Response(
      JSON.stringify({ 
        error: error instanceof Error ? error.message : 'Unknown error' 
      }),
      { 
        status: 500,
        headers: { "Content-Type": "application/json" } 
      },
    );
  }
})

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/sync-scheduler' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
