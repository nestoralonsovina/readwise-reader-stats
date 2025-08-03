create table "public"."documents" (
    "id" uuid not null default gen_random_uuid(),
    "readwise_id" text not null,
    "user_id" uuid,
    "title" text not null,
    "author" text,
    "source" text,
    "category" text,
    "url" text,
    "word_count" integer,
    "created_at" timestamp with time zone default now(),
    "updated_at" timestamp with time zone default now(),
    "last_synced_at" timestamp with time zone,
    "reading_progress" numeric(3,2) default 0,
    "first_opened_at" timestamp with time zone,
    "last_opened_at" timestamp with time zone,
    "archived" boolean default false
);


create table "public"."reading_events" (
    "time" timestamp with time zone not null default now(),
    "user_id" uuid not null,
    "document_id" uuid not null,
    "event_type" text not null,
    "reading_progress" numeric(3,2),
    "session_duration_seconds" integer,
    "words_read" integer,
    "device_type" text,
    "location" text
);


create table "public"."sync_queue" (
    "id" uuid not null default gen_random_uuid(),
    "user_id" uuid,
    "sync_type" text not null,
    "resource_id" text,
    "priority" integer default 5,
    "attempts" integer default 0,
    "max_attempts" integer default 3,
    "status" text default 'pending'::text,
    "scheduled_for" timestamp with time zone default now(),
    "processed_at" timestamp with time zone,
    "error_message" text,
    "created_at" timestamp with time zone default now()
);


create table "public"."sync_status" (
    "id" uuid not null default gen_random_uuid(),
    "user_id" uuid,
    "last_full_sync" timestamp with time zone,
    "last_incremental_sync" timestamp with time zone,
    "next_cursor" text,
    "sync_in_progress" boolean default false,
    "documents_synced" integer default 0,
    "highlights_synced" integer default 0,
    "failed_attempts" integer default 0,
    "last_error" text,
    "created_at" timestamp with time zone default now(),
    "updated_at" timestamp with time zone default now()
);


CREATE UNIQUE INDEX documents_pkey ON public.documents USING btree (id);

CREATE UNIQUE INDEX documents_readwise_id_key ON public.documents USING btree (readwise_id);

CREATE INDEX idx_documents_category ON public.documents USING btree (category);

CREATE INDEX idx_documents_last_synced ON public.documents USING btree (last_synced_at);

CREATE INDEX idx_documents_user_id ON public.documents USING btree (user_id);

CREATE INDEX idx_queue_status_scheduled ON public.sync_queue USING btree (status, scheduled_for);

CREATE INDEX idx_queue_user_status ON public.sync_queue USING btree (user_id, status);

CREATE UNIQUE INDEX reading_events_pkey ON public.reading_events USING btree ("time", user_id, document_id);

CREATE UNIQUE INDEX sync_queue_pkey ON public.sync_queue USING btree (id);

CREATE UNIQUE INDEX sync_status_pkey ON public.sync_status USING btree (id);

CREATE UNIQUE INDEX sync_status_user_id_key ON public.sync_status USING btree (user_id);

alter table "public"."documents" add constraint "documents_pkey" PRIMARY KEY using index "documents_pkey";

alter table "public"."reading_events" add constraint "reading_events_pkey" PRIMARY KEY using index "reading_events_pkey";

alter table "public"."sync_queue" add constraint "sync_queue_pkey" PRIMARY KEY using index "sync_queue_pkey";

alter table "public"."sync_status" add constraint "sync_status_pkey" PRIMARY KEY using index "sync_status_pkey";

alter table "public"."documents" add constraint "documents_readwise_id_key" UNIQUE using index "documents_readwise_id_key";

alter table "public"."documents" add constraint "documents_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) not valid;

alter table "public"."documents" validate constraint "documents_user_id_fkey";

alter table "public"."reading_events" add constraint "reading_events_document_id_fkey" FOREIGN KEY (document_id) REFERENCES documents(id) not valid;

alter table "public"."reading_events" validate constraint "reading_events_document_id_fkey";

alter table "public"."sync_queue" add constraint "sync_queue_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) not valid;

alter table "public"."sync_queue" validate constraint "sync_queue_user_id_fkey";

alter table "public"."sync_status" add constraint "sync_status_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) not valid;

alter table "public"."sync_status" validate constraint "sync_status_user_id_fkey";

alter table "public"."sync_status" add constraint "sync_status_user_id_key" UNIQUE using index "sync_status_user_id_key";

grant delete on table "public"."documents" to "anon";

grant insert on table "public"."documents" to "anon";

grant references on table "public"."documents" to "anon";

grant select on table "public"."documents" to "anon";

grant trigger on table "public"."documents" to "anon";

grant truncate on table "public"."documents" to "anon";

grant update on table "public"."documents" to "anon";

grant delete on table "public"."documents" to "authenticated";

grant insert on table "public"."documents" to "authenticated";

grant references on table "public"."documents" to "authenticated";

grant select on table "public"."documents" to "authenticated";

grant trigger on table "public"."documents" to "authenticated";

grant truncate on table "public"."documents" to "authenticated";

grant update on table "public"."documents" to "authenticated";

grant delete on table "public"."documents" to "service_role";

grant insert on table "public"."documents" to "service_role";

grant references on table "public"."documents" to "service_role";

grant select on table "public"."documents" to "service_role";

grant trigger on table "public"."documents" to "service_role";

grant truncate on table "public"."documents" to "service_role";

grant update on table "public"."documents" to "service_role";

grant delete on table "public"."reading_events" to "anon";

grant insert on table "public"."reading_events" to "anon";

grant references on table "public"."reading_events" to "anon";

grant select on table "public"."reading_events" to "anon";

grant trigger on table "public"."reading_events" to "anon";

grant truncate on table "public"."reading_events" to "anon";

grant update on table "public"."reading_events" to "anon";

grant delete on table "public"."reading_events" to "authenticated";

grant insert on table "public"."reading_events" to "authenticated";

grant references on table "public"."reading_events" to "authenticated";

grant select on table "public"."reading_events" to "authenticated";

grant trigger on table "public"."reading_events" to "authenticated";

grant truncate on table "public"."reading_events" to "authenticated";

grant update on table "public"."reading_events" to "authenticated";

grant delete on table "public"."reading_events" to "service_role";

grant insert on table "public"."reading_events" to "service_role";

grant references on table "public"."reading_events" to "service_role";

grant select on table "public"."reading_events" to "service_role";

grant trigger on table "public"."reading_events" to "service_role";

grant truncate on table "public"."reading_events" to "service_role";

grant update on table "public"."reading_events" to "service_role";

grant delete on table "public"."sync_queue" to "anon";

grant insert on table "public"."sync_queue" to "anon";

grant references on table "public"."sync_queue" to "anon";

grant select on table "public"."sync_queue" to "anon";

grant trigger on table "public"."sync_queue" to "anon";

grant truncate on table "public"."sync_queue" to "anon";

grant update on table "public"."sync_queue" to "anon";

grant delete on table "public"."sync_queue" to "authenticated";

grant insert on table "public"."sync_queue" to "authenticated";

grant references on table "public"."sync_queue" to "authenticated";

grant select on table "public"."sync_queue" to "authenticated";

grant trigger on table "public"."sync_queue" to "authenticated";

grant truncate on table "public"."sync_queue" to "authenticated";

grant update on table "public"."sync_queue" to "authenticated";

grant delete on table "public"."sync_queue" to "service_role";

grant insert on table "public"."sync_queue" to "service_role";

grant references on table "public"."sync_queue" to "service_role";

grant select on table "public"."sync_queue" to "service_role";

grant trigger on table "public"."sync_queue" to "service_role";

grant truncate on table "public"."sync_queue" to "service_role";

grant update on table "public"."sync_queue" to "service_role";

grant delete on table "public"."sync_status" to "anon";

grant insert on table "public"."sync_status" to "anon";

grant references on table "public"."sync_status" to "anon";

grant select on table "public"."sync_status" to "anon";

grant trigger on table "public"."sync_status" to "anon";

grant truncate on table "public"."sync_status" to "anon";

grant update on table "public"."sync_status" to "anon";

grant delete on table "public"."sync_status" to "authenticated";

grant insert on table "public"."sync_status" to "authenticated";

grant references on table "public"."sync_status" to "authenticated";

grant select on table "public"."sync_status" to "authenticated";

grant trigger on table "public"."sync_status" to "authenticated";

grant truncate on table "public"."sync_status" to "authenticated";

grant update on table "public"."sync_status" to "authenticated";

grant delete on table "public"."sync_status" to "service_role";

grant insert on table "public"."sync_status" to "service_role";

grant references on table "public"."sync_status" to "service_role";

grant select on table "public"."sync_status" to "service_role";

grant trigger on table "public"."sync_status" to "service_role";

grant truncate on table "public"."sync_status" to "service_role";

grant update on table "public"."sync_status" to "service_role";


