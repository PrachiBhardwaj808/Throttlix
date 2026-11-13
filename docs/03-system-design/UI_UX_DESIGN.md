# UI/UX Design & Dashboard Specifications — Throttlix

## 1. Executive Summary & Design Vision
The **Throttlix Dashboard** is an administrative and operational web interface built with **React 19**, **TypeScript**, and **Tailwind CSS**. 

Its purpose is to provide system operators with high-clarity, real-time observability over API traffic, rejection trends, client activity, and quota policies without visual clutter or unnecessary complexity.

---

## 2. Information Architecture & Layout Structure

```text
┌────────────────────────────────────────────────────────────────────────┐
│  Throttlix Platform  │  [Dashboard]   [Clients & Keys]   [Policies]    │
├────────────────────────────────────────────────────────────────────────┤
│  KPI SUMMARY CARDS                                                     │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │ Total Reqs   │ │ Allowed Reqs │ │ 429 Rejections│ │ Avg Latency  │   │
│  │ 128,450      │ │ 124,200 (97%)│ │ 4,250 (3%)   │ │ 3.4 ms       │   │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘   │
├────────────────────────────────────────────────────────────────────────┤
│  TRAFFIC VOLUME & REJECTIONS (Recharts Area / Bar Chart)               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  [██████ Allowed Traffic]    [░░░░░░ 429 Rejections]             │  │
│  │  ──────────────────────────────────────────────────────────────  │  │
│  │  00:00        04:00        08:00        12:00        16:00       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────────────┤
│  CLIENT ACTIVITY TABLE                    │ RECENT REQUEST STREAM      │
│  ┌──────────────────────────────────────┐ │ ┌────────────────────────┐ │
│  │ Client Name │ Active Policy │ Vol   │ │ │ 14:02:11 /orders 200   │ │
│  │ Acme Corp   │ TokenBucket   │ 42.1k │ │ │ 14:02:11 /orders 429   │ │
│  │ Beta Inc    │ SlidingWindow │ 18.4k │ │ │ 14:02:10 /orders 200   │ │
│  └──────────────────────────────────────┘ │ └────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Core Screen Specifications

### 3.1 KPI Metrics Bar
- **Total Ingress Requests:** Aggregate volume over the selected observation window.
- **Allowed Traffic:** Volume and percentage of requests meeting policy criteria.
- **Throttled Rejections (429):** Explicit count and percentage of rejected burst calls.
- **Average Gateway Latency:** Internal evaluation duration in milliseconds.

### 3.2 Time-Series Traffic Chart (Recharts)
- Dual-series stacked chart rendering allowed vs. throttled request counts over time.
- Tooltips display exact counts per 5-minute sampling interval.

### 3.3 Client Usage & Ranking Table
- Sortable tabular view listing registered clients, active key counts, linked policies, and 24-hour total request volumes.
- Provides one-click action to view client-specific policy details or revoke keys.

---

## 4. Design System & Theme
- **Styling Framework:** Tailwind CSS v4.
- **Color Palette:**
  - Background: Neutral slate (`bg-slate-50` / `dark:bg-slate-900`)
  - Primary / Allowed: Emerald green (`emerald-500`)
  - Warnings / 429 Rejections: Amber / Rose (`rose-500`)
  - Text: High-contrast neutral (`text-slate-800` / `dark:text-slate-100`)
- **Typography:** System font stack (`system-ui, -apple-system, Roboto, sans-serif`) optimized for legible data tables and monospaced timestamps.
