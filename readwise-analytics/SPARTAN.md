# Spartan UI Components Reference

This project uses [Spartan UI](https://www.spartan.ng/) - a collection of Angular components built on Radix UI primitives with Tailwind CSS styling.

## Architecture

Spartan follows a two-layer design:

1. **`@spartan-ng/brain`** - Headless, unstyled base components (accessibility, behavior)
2. **`libs/ui/*`** - Styled wrapper directives using Tailwind + CVA (class-variance-authority)

All styled components use the `hlm` utility for class merging:
```typescript
import { hlm } from '@spartan-ng/helm/utils';
```

---

## Quick Reference

| Component | Import | Selector | Variants |
|-----------|--------|----------|----------|
| Button | `HlmButton` | `hlmBtn` | default, destructive, outline, secondary, ghost, link |
| Badge | `HlmBadge` | `hlmBadge` | default, secondary, destructive, outline |
| Card | `HlmCardImports` | `hlmCard` | - |
| Alert | `HlmAlert` | `hlmAlert` | default, destructive |
| Progress | `HlmProgressImports` | `hlmProgress` | - |
| Toggle | `HlmToggle` | `hlmToggle` | default, outline |
| Toggle Group | `HlmToggleGroup` | `hlmToggleGroup` | default, outline |
| Icon | `HlmIconImports` | `hlm` on `ng-icon` | sizes: xs, sm, base, lg, xl |
| Input | `HlmInput` | `hlmInput` | error: auto, true |
| Separator | `HlmSeparator` | `hlmSeparator` | orientation: horizontal, vertical |
| Skeleton | `HlmSkeleton` | `hlmSkeleton` | - |
| Spinner | `HlmSpinner` | `hlm-spinner` | - |

---

## Component Details

### Button

```typescript
import { HlmButton } from '@libs/ui/button';

@Component({
  imports: [HlmButton],
  template: `
    <button hlmBtn>Default</button>
    <button hlmBtn variant="outline">Outline</button>
    <button hlmBtn variant="destructive">Delete</button>
    <button hlmBtn variant="ghost" size="icon">
      <ng-icon hlm name="lucideX" size="sm"/>
    </button>
  `
})
```

**Variants**: `default` | `destructive` | `outline` | `secondary` | `ghost` | `link`
**Sizes**: `default` | `sm` | `lg` | `icon` | `icon-sm` | `icon-lg`

### Badge

```typescript
import { HlmBadge } from '@libs/ui/badge';

@Component({
  imports: [HlmBadge],
  template: `
    <span hlmBadge>Default</span>
    <span hlmBadge variant="secondary">Secondary</span>
    <span hlmBadge variant="outline">Outline</span>
    <span hlmBadge variant="destructive">Error</span>
  `
})
```

**Variants**: `default` | `secondary` | `destructive` | `outline`

### Card

```typescript
import { HlmCardImports } from '@libs/ui/card';

@Component({
  imports: [...HlmCardImports],
  template: `
    <div hlmCard>
      <div hlmCardHeader>
        <h3 hlmCardTitle>Title</h3>
        <p hlmCardDescription>Description</p>
      </div>
      <div hlmCardContent>
        Content here
      </div>
      <div hlmCardFooter>
        <button hlmBtn>Action</button>
      </div>
    </div>
  `
})
```

**Sub-components**: `HlmCard`, `HlmCardHeader`, `HlmCardTitle`, `HlmCardDescription`, `HlmCardContent`, `HlmCardFooter`, `HlmCardAction`

### Alert

```typescript
import { HlmAlert, HlmAlertTitle, HlmAlertDescription, HlmAlertIcon } from '@libs/ui/alert';

@Component({
  imports: [HlmAlert, HlmAlertTitle, HlmAlertDescription, HlmAlertIcon],
  template: `
    <div hlmAlert>
      <ng-icon hlmAlertIcon name="lucideInfo"/>
      <h4 hlmAlertTitle>Heads up!</h4>
      <p hlmAlertDescription>This is an alert message.</p>
    </div>

    <div hlmAlert variant="destructive">
      <ng-icon hlmAlertIcon name="lucideAlertCircle"/>
      <h4 hlmAlertTitle>Error</h4>
      <p hlmAlertDescription>Something went wrong.</p>
    </div>
  `
})
```

**Variants**: `default` | `destructive`

### Progress

```typescript
import { HlmProgressImports } from '@libs/ui/progress';
import { BrnProgressComponent } from '@spartan-ng/brain/progress';

@Component({
  imports: [...HlmProgressImports, BrnProgressComponent],
  template: `
    <brn-progress hlmProgress [value]="65">
      <div hlmProgressIndicator></div>
    </brn-progress>
  `
})
```

### Toggle

```typescript
import { HlmToggle } from '@libs/ui/toggle';

@Component({
  imports: [HlmToggle],
  template: `
    <button hlmToggle>
      <ng-icon hlm name="lucideBold" size="sm"/>
    </button>
    <button hlmToggle variant="outline" size="sm">
      Small
    </button>
  `
})
```

**Variants**: `default` | `outline`
**Sizes**: `default` | `sm` | `lg`

### Toggle Group

```typescript
import { HlmToggleGroup, HlmToggleGroupItem } from '@libs/ui/toggle-group';
import { BrnToggleGroup } from '@spartan-ng/brain/toggle-group';

@Component({
  imports: [HlmToggleGroup, HlmToggleGroupItem, BrnToggleGroup],
  template: `
    <div hlmToggleGroup type="single" [value]="selected">
      <button hlmToggleGroupItem value="week">Week</button>
      <button hlmToggleGroupItem value="month">Month</button>
      <button hlmToggleGroupItem value="year">Year</button>
    </div>
  `
})
```

**Props**: `type="single"` | `type="multiple"`, `value`, `nullable`, `disabled`

### Icon

```typescript
import { HlmIconImports } from '@libs/ui/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideHome, lucideSettings, lucideSun, lucideMoon } from '@ng-icons/lucide';

@Component({
  imports: [...HlmIconImports],
  providers: [provideIcons({ lucideHome, lucideSettings, lucideSun, lucideMoon })],
  template: `
    <ng-icon hlm name="lucideHome" size="sm"/>
    <ng-icon hlm name="lucideSettings" size="base"/>
    <ng-icon hlm name="lucideSun" size="lg"/>
  `
})
```

**Sizes**: `xs` (12px) | `sm` (16px) | `base` (24px) | `lg` (32px) | `xl` (48px) | `none`

### Input

```typescript
import { HlmInput } from '@libs/ui/input';

@Component({
  imports: [HlmInput],
  template: `
    <input hlmInput type="text" placeholder="Enter text..."/>
    <input hlmInput type="email" [error]="true"/>
  `
})
```

**Error states**: `auto` (follows form validation) | `true` (always show error style)

### Separator

```typescript
import { HlmSeparator } from '@libs/ui/separator';

@Component({
  imports: [HlmSeparator],
  template: `
    <div hlmSeparator></div>
    <div hlmSeparator orientation="vertical" class="h-6"></div>
  `
})
```

**Orientation**: `horizontal` (default) | `vertical`

### Skeleton

```typescript
import { HlmSkeleton } from '@libs/ui/skeleton';

@Component({
  imports: [HlmSkeleton],
  template: `
    <div hlmSkeleton class="h-4 w-[250px]"></div>
    <div hlmSkeleton class="h-12 w-12 rounded-full"></div>
  `
})
```

### Spinner

```typescript
import { HlmSpinner } from '@libs/ui/spinner';

@Component({
  imports: [HlmSpinner],
  template: `
    <hlm-spinner/>
    <hlm-spinner class="text-primary size-8"/>
  `
})
```

---

## Sidebar System

Full sidebar navigation system with 23 sub-components.

```typescript
import { HlmSidebarImports } from '@libs/ui/sidebar';

@Component({
  imports: [...HlmSidebarImports],
  template: `
    <div hlmSidebarWrapper>
      <aside hlmSidebar>
        <div hlmSidebarHeader>
          <!-- Logo -->
        </div>
        <div hlmSidebarContent>
          <div hlmSidebarGroup>
            <span hlmSidebarGroupLabel>Navigation</span>
            <div hlmSidebarGroupContent>
              <ul hlmSidebarMenu>
                <li hlmSidebarMenuItem>
                  <a hlmSidebarMenuButton routerLink="/dashboard">
                    <ng-icon hlm name="lucideLayoutDashboard" size="sm"/>
                    <span>Dashboard</span>
                  </a>
                </li>
                <li hlmSidebarMenuItem>
                  <a hlmSidebarMenuButton routerLink="/settings">
                    <ng-icon hlm name="lucideSettings" size="sm"/>
                    <span>Settings</span>
                    <span hlmSidebarMenuBadge>New</span>
                  </a>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div hlmSidebarFooter>
          <!-- Footer content -->
        </div>
      </aside>
      <div hlmSidebarInset>
        <router-outlet/>
      </div>
    </div>
  `
})
```

**Sub-components**:
- Layout: `HlmSidebar`, `HlmSidebarWrapper`, `HlmSidebarContent`, `HlmSidebarHeader`, `HlmSidebarFooter`, `HlmSidebarInset`
- Groups: `HlmSidebarGroup`, `HlmSidebarGroupLabel`, `HlmSidebarGroupContent`, `HlmSidebarGroupAction`
- Menu: `HlmSidebarMenu`, `HlmSidebarMenuItem`, `HlmSidebarMenuButton`, `HlmSidebarMenuBadge`, `HlmSidebarMenuAction`
- Sub-menu: `HlmSidebarMenuSub`, `HlmSidebarMenuSubItem`, `HlmSidebarMenuSubButton`
- Utilities: `HlmSidebarInput`, `HlmSidebarRail`, `HlmSidebarSeparator`, `HlmSidebarTrigger`, `HlmSidebarMenuSkeleton`

---

## All Installed Components

| Category | Components |
|----------|------------|
| **Buttons** | button, button-group, toggle, toggle-group |
| **Display** | badge, avatar, card, alert, empty, typography |
| **Forms** | input, input-group, input-otp, textarea, checkbox, radio-group, select, switch, label, field, form-field |
| **Layout** | separator, scroll-area, resizable, aspect-ratio |
| **Navigation** | sidebar, tabs, breadcrumb, pagination, navigation-menu, menubar |
| **Overlays** | dialog, sheet, popover, tooltip, hover-card, dropdown-menu, context-menu, command |
| **Feedback** | progress, skeleton, spinner, sonner (toasts) |
| **Data** | table, accordion, collapsible, carousel, calendar, date-picker, slider |
| **Misc** | icon, kbd, item, autocomplete |

---

## Theming

Theme variables are defined in `src/styles.css` using OKLCh color space:

```css
:root {
  --background: oklch(1 0 0);
  --foreground: oklch(0.145 0 0);
  --primary: oklch(0.205 0 0);
  --primary-foreground: oklch(0.985 0 0);
  --secondary: oklch(0.97 0 0);
  --muted: oklch(0.97 0 0);
  --accent: oklch(0.97 0 0);
  --destructive: oklch(0.577 0.245 27.325);
  --border: oklch(0.922 0 0);
  --ring: oklch(0.708 0 0);
  --brand: oklch(0.769 0.188 70.08);  /* amber */
  /* ... */
}

:root.dark {
  /* Dark mode overrides */
}
```

Components automatically respond to these variables.

---

## Import Patterns

Each component exports an `Hlm*Imports` array for convenience:

```typescript
// Individual imports
import { HlmButton } from '@libs/ui/button';
import { HlmBadge } from '@libs/ui/badge';

// Bulk imports
import { HlmCardImports } from '@libs/ui/card';
import { HlmSidebarImports } from '@libs/ui/sidebar';
import { HlmProgressImports } from '@libs/ui/progress';
import { HlmIconImports } from '@libs/ui/icon';

@Component({
  imports: [
    HlmButton,
    HlmBadge,
    ...HlmCardImports,
    ...HlmProgressImports,
  ]
})
```

---

## Future Adoption Opportunities

| Current Custom Component | Spartan Replacement | Priority |
|--------------------------|---------------------|----------|
| `coming-soon-badge` | `HlmBadge variant="secondary"` | Quick win |
| Dashboard buttons | `HlmButton` | Quick win |
| `period-toggle` | `HlmToggleGroup` | Quick win |
| Progress bars | `HlmProgress` | Quick win |
| All card containers | `HlmCard` | Medium |
| Inline SVG icons | `HlmIcon` + Lucide | Medium |
| Custom sidebar | `HlmSidebar*` system | Later |

---

## File Locations

- **Component libs**: `libs/ui/*/`
- **Brain (headless)**: `@spartan-ng/brain/*`
- **Theme config**: `src/styles.css`
- **Utility function**: `libs/ui/utils/src/lib/hlm.ts`
