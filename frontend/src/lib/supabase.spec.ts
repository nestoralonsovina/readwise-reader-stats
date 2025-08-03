import { describe, it, expect } from "vitest";
import { supabase } from "./supabase.ts";

describe("Supabase Client", () => {
  it.todo("should initialize with correct URL and key");
  it.todo("should have auth client available");
  
  it("should be defined", () => {
    expect(supabase).toBeDefined();
  });
});