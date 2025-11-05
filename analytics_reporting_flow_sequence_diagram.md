# Analytics & Reporting Flow - Sequence Diagram

**Note:** Authentication is handled by the API Gateway before requests reach the API.

This sequence diagram illustrates the analytics and reporting capabilities of the Document Hub API, including document statistics, template usage analytics, and performance monitoring.

## Mermaid Sequence Diagram - Document Statistics

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Dashboard
    participant API as Document Hub API
    participant AnalyticsService as Analytics<br/>Service
    participant CacheService as Redis<br/>Cache
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Retrieve Document Statistics

    Admin->>Client: View Dashboard<br/>(Document Statistics)
    Client->>+API: GET /analytics/documents/stats?<br/>date_from=2024-01-01<br/>&date_to=2024-11-01<br/>&group_by=type<br/>Authorization: Bearer <token>

    API->>API: Validate Permissions<br/>(requires: analytics:read)

    alt Insufficient Permissions
        API-->>Client: 403 Forbidden
        Client-->>Admin: Access Denied
    end

    API->>+AnalyticsService: Get Document Statistics

    AnalyticsService->>+CacheService: Check Cache<br/>GET analytics:documents:2024-01-01:2024-11-01:type

    alt Cache Hit
        CacheService-->>AnalyticsService: Cached Statistics<br/>(15 min old)
        Note over AnalyticsService: Return cached data
        AnalyticsService-->>API: Statistics
    end

    alt Cache Miss
        CacheService-->>-AnalyticsService: Cache Miss

        Note over AnalyticsService,DB: Execute Multiple Analytical Queries

        par Total Document Count
            AnalyticsService->>+DB: SELECT COUNT(*)<br/>FROM documents<br/>WHERE deleted_at IS NULL<br/>AND uploaded_at BETWEEN ? AND ?
            DB-->>-AnalyticsService: Total: 1,245,678
        and Customer-Specific vs Shared
            AnalyticsService->>+DB: SELECT<br/>  SUM(CASE WHEN is_shared = false<br/>      THEN 1 ELSE 0 END) as customer_specific,<br/>  SUM(CASE WHEN is_shared = true<br/>      THEN 1 ELSE 0 END) as shared<br/>FROM documents<br/>WHERE deleted_at IS NULL
            DB-->>-AnalyticsService: Customer: 1,245,100<br/>Shared: 578
        and By Document Type
            AnalyticsService->>+DB: SELECT<br/>  document_type,<br/>  COUNT(*) as count<br/>FROM documents<br/>WHERE deleted_at IS NULL<br/>AND uploaded_at BETWEEN ? AND ?<br/>GROUP BY document_type<br/>ORDER BY count DESC<br/>LIMIT 10
            DB-->>-AnalyticsService: Type Breakdown:<br/>ACCOUNT_STATEMENT: 456,789<br/>LOAN_APPLICATION: 123,456<br/>...
        and By Document Category
            AnalyticsService->>+DB: SELECT<br/>  document_category,<br/>  COUNT(*) as count<br/>FROM documents<br/>WHERE deleted_at IS NULL<br/>GROUP BY document_category<br/>ORDER BY count DESC
            DB-->>-AnalyticsService: Category Breakdown:<br/>BANKING: 678,901<br/>LENDING: 234,567<br/>...
        and Storage Usage
            AnalyticsService->>+DB: SELECT<br/>  SUM(file_size_bytes) as total_bytes,<br/>  AVG(file_size_bytes) as avg_bytes,<br/>  MAX(file_size_bytes) as max_bytes<br/>FROM documents<br/>WHERE deleted_at IS NULL
            DB-->>-AnalyticsService: Total: 5,368,709,120 bytes<br/>Avg: 4,310 bytes<br/>Max: 52,428,800 bytes
        and Recent Activity
            AnalyticsService->>+DB: SELECT<br/>  COUNT(CASE WHEN uploaded_at >=<br/>    CURRENT_DATE THEN 1 END)<br/>    as today,<br/>  COUNT(CASE WHEN uploaded_at >=<br/>    CURRENT_DATE - 7 THEN 1 END)<br/>    as this_week,<br/>  COUNT(CASE WHEN uploaded_at >=<br/>    DATE_TRUNC('month', CURRENT_DATE)<br/>    THEN 1 END) as this_month<br/>FROM documents
            DB-->>-AnalyticsService: Today: 1,245<br/>Week: 8,901<br/>Month: 34,567
        end

        AnalyticsService->>AnalyticsService: Aggregate Results<br/>Calculate percentages<br/>Format response

        AnalyticsService->>+CacheService: Store in Cache<br/>SET analytics:documents:*<br/>TTL: 15 minutes
        CacheService-->>-AnalyticsService: Cached

        AnalyticsService-->>-API: Comprehensive Statistics
    end

    API-->>-Client: 200 OK<br/>{<br/>  total_documents: 1245678,<br/>  customer_specific_documents: 1245100,<br/>  shared_documents: 578,<br/>  by_type: [{type, count}, ...],<br/>  by_category: [{category, count}, ...],<br/>  storage_usage: {<br/>    total_bytes, total_gb,<br/>    avg_bytes, max_bytes},<br/>  recent_activity: {<br/>    documents_uploaded_today: 1245,<br/>    documents_uploaded_this_week: 8901,<br/>    documents_uploaded_this_month: 34567<br/>  },<br/>  cached_at: "2024-11-01T12:00:00Z"<br/>}

    Client->>Client: Render Charts:<br/>- Pie chart by type<br/>- Bar chart by category<br/>- Line chart activity trends<br/>- Storage gauge

    Client-->>Admin: Display Dashboard<br/>(Interactive Charts)

    Note over Admin,DB: Query Performance: <500ms (with cache: <10ms)
```

## Mermaid Sequence Diagram - Template Usage Statistics

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Dashboard
    participant API as Document Hub API
    participant AnalyticsService as Analytics<br/>Service
    participant CacheService as Redis<br/>Cache
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Retrieve Template Usage Statistics

    Admin->>Client: View Template Analytics
    Client->>+API: GET /analytics/templates/usage<br/>Authorization: Bearer <token>

    API->>API: Validate Permissions

    API->>+AnalyticsService: Get Template Usage Stats

    AnalyticsService->>+CacheService: Check Cache<br/>GET analytics:templates:usage

    alt Cache Hit
        CacheService-->>AnalyticsService: Cached Data
        AnalyticsService-->>API: Template Stats
    end

    alt Cache Miss
        CacheService-->>-AnalyticsService: Cache Miss

        par Template Counts
            AnalyticsService->>+DB: SELECT<br/>  COUNT(*) as total,<br/>  SUM(CASE WHEN status = 'active'<br/>      THEN 1 ELSE 0 END) as active,<br/>  SUM(CASE WHEN status = 'deprecated'<br/>      THEN 1 ELSE 0 END) as deprecated,<br/>  SUM(CASE WHEN status = 'draft'<br/>      THEN 1 ELSE 0 END) as draft<br/>FROM templates<br/>WHERE deleted_at IS NULL
            DB-->>-AnalyticsService: Total: 400<br/>Active: 385<br/>Deprecated: 15<br/>Draft: 0
        and Templates with Document Counts
            AnalyticsService->>+DB: SELECT<br/>  t.template_code,<br/>  t.template_name,<br/>  t.version_number,<br/>  t.status,<br/>  COUNT(d.document_id) as document_count<br/>FROM templates t<br/>LEFT JOIN documents d<br/>  ON t.template_id = d.template_id<br/>  AND d.deleted_at IS NULL<br/>GROUP BY t.template_id,<br/>  t.template_code,<br/>  t.template_name<br/>ORDER BY document_count DESC<br/>LIMIT 20
            DB-->>-AnalyticsService: Top Templates:<br/>ACCOUNT_STATEMENT: 456,789 docs<br/>LOAN_APPLICATION: 123,456 docs<br/>...
        and Unused Templates
            AnalyticsService->>+DB: SELECT<br/>  template_code,<br/>  template_name,<br/>  status,<br/>  created_at<br/>FROM templates t<br/>WHERE NOT EXISTS (<br/>  SELECT 1 FROM documents d<br/>  WHERE d.template_id = t.template_id<br/>  AND d.deleted_at IS NULL<br/>)<br/>AND t.status = 'active'
            DB-->>-AnalyticsService: Unused: 12 templates
        and Version Distribution
            AnalyticsService->>+DB: SELECT<br/>  template_code,<br/>  COUNT(DISTINCT version_number)<br/>    as version_count,<br/>  MAX(version_number) as latest_version<br/>FROM templates<br/>GROUP BY template_code<br/>HAVING COUNT(*) > 1<br/>ORDER BY version_count DESC<br/>LIMIT 10
            DB-->>-AnalyticsService: Multi-version Templates:<br/>LOAN_APPLICATION: 8 versions<br/>...
        end

        AnalyticsService->>AnalyticsService: Aggregate & Calculate:<br/>- Usage percentages<br/>- Adoption rates<br/>- Version metrics

        AnalyticsService->>+CacheService: Store in Cache<br/>TTL: 30 minutes
        CacheService-->>-AnalyticsService: Cached

        AnalyticsService-->>-API: Template Statistics
    end

    API-->>-Client: 200 OK<br/>{<br/>  total_templates: 400,<br/>  active_templates: 385,<br/>  deprecated_templates: 15,<br/>  draft_templates: 0,<br/>  templates_with_documents: [<br/>    {template_code, template_name,<br/>     version_number, document_count,<br/>     status}, ...<br/>  ],<br/>  unused_templates: 12,<br/>  version_statistics: {<br/>    avg_versions_per_template: 2.3,<br/>    max_versions: 8,<br/>    templates_with_multiple_versions: 45<br/>  }<br/>}

    Client->>Client: Render Visualizations:<br/>- Template usage heat map<br/>- Version timeline<br/>- Unused templates list<br/>- Top templates ranking

    Client-->>Admin: Display Template Analytics

    Note over Admin,DB: Insights for template optimization
```

## Mermaid Sequence Diagram - Real-time Dashboard with WebSocket

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Dashboard<br/>(WebSocket)
    participant WebSocketServer as WebSocket<br/>Server
    participant EventBus as Event Bus<br/>(Kafka/RabbitMQ)
    participant API as Document Hub API
    participant AnalyticsService as Analytics<br/>Service
    participant DB as PostgreSQL<br/>Database

    Note over Admin,EventBus: Real-time Analytics Dashboard

    Admin->>Client: Open Analytics Dashboard
    Client->>+WebSocketServer: WebSocket Connect<br/>ws://api.documenthub.com/analytics<br/>Authorization: Bearer <token>

    WebSocketServer->>WebSocketServer: Validate JWT Token

    WebSocketServer-->>-Client: Connection Established<br/>{status: "connected"}

    Client->>+WebSocketServer: Subscribe to Analytics<br/>{channel: "document_stats"}

    WebSocketServer->>+EventBus: Subscribe to Events<br/>- document.uploaded<br/>- document.deleted<br/>- template.created

    EventBus-->>-WebSocketServer: Subscription Active

    WebSocketServer-->>-Client: Subscription Confirmed

    Note over Admin,DB: Initial Data Load

    Client->>+API: GET /analytics/documents/stats
    API->>+AnalyticsService: Get Current Stats
    AnalyticsService->>+DB: Query Statistics
    DB-->>-AnalyticsService: Current Data
    AnalyticsService-->>-API: Statistics
    API-->>-Client: 200 OK (Initial Data)

    Client->>Client: Render Dashboard<br/>(Initial State)

    Note over Admin,EventBus: Real-time Events

    loop Real-time Updates (every event)
        Note over API,EventBus: Document Uploaded Event

        API->>+EventBus: Publish Event<br/>{type: "document.uploaded",<br/>document_type: "LOAN_APPLICATION",<br/>file_size_bytes: 1048576,<br/>timestamp}

        EventBus->>+WebSocketServer: Forward Event<br/>(to subscribed clients)

        WebSocketServer->>+AnalyticsService: Get Updated Stats<br/>(incremental)
        AnalyticsService->>AnalyticsService: Update Counters:<br/>- total_documents++<br/>- type_count[LOAN_APPLICATION]++<br/>- storage_bytes += 1048576

        AnalyticsService-->>-WebSocketServer: Updated Metrics

        WebSocketServer-->>-Client: WebSocket Message<br/>{<br/>  type: "stats_update",<br/>  data: {<br/>    total_documents: 1245679,<br/>    by_type: {<br/>      LOAN_APPLICATION: 123457<br/>    },<br/>    storage_usage: {<br/>      total_bytes: 5369757696<br/>    }<br/>  },<br/>  timestamp<br/>}

        Client->>Client: Update Dashboard:<br/>- Increment counters<br/>- Update charts<br/>- Show notification<br/>("New document uploaded")

        Client-->>Admin: Live Dashboard Update<br/>(animated transitions)

        EventBus-->>-API: Event Published
    end

    Note over Admin,DB: Dashboard refreshes in real-time
```

## Flow Descriptions

### Document Statistics Flow

1. **Authorization** (Step 1)
   - Admin opens analytics dashboard
   - API Gateway validates JWT token before request reaches API
   - Require `analytics:read` permission
   - If unauthorized → 403 Forbidden

2. **Cache Check** (Steps 2-4)
   - Check Redis cache for recent results
   - Cache key includes date range and grouping
   - If cache hit (< 15 min old), return immediately
   - If cache miss, proceed to database queries

3. **Parallel Analytical Queries** (Steps 5-15)
   Execute multiple queries in parallel for performance:

   - **Total Document Count**: Count all non-deleted documents
   - **Customer-Specific vs Shared**: Breakdown by `is_shared` flag
   - **By Document Type**: Top 10 document types with counts
   - **By Document Category**: All categories with counts
   - **Storage Usage**: Sum, average, and max file sizes
   - **Recent Activity**: Counts for today, this week, this month

4. **Aggregation** (Steps 16-17)
   - Combine all query results
   - Calculate percentages and derived metrics
   - Format for API response

5. **Cache Storage** (Steps 18-19)
   - Store results in Redis cache
   - Set TTL to 15 minutes
   - Subsequent requests are much faster

6. **Response & Visualization** (Steps 20-23)
   - Return comprehensive statistics
   - Client renders interactive charts
   - Display to admin

### Template Usage Statistics Flow

1. **Authorization** (Step 1)
   - Admin requests template analytics
   - API Gateway validates JWT token before request reaches API

2. **Cache Check** (Steps 2-4)
   - Check for cached template statistics
   - 30-minute TTL (templates change less frequently)

3. **Template Queries** (Steps 5-13)
   Execute specialized template queries:

   - **Template Counts**: Total, active, deprecated, draft
   - **Usage Ranking**: Templates ordered by document count
   - **Unused Templates**: Active templates with no documents
   - **Version Distribution**: Templates with multiple versions

4. **Analysis** (Steps 14-15)
   - Calculate adoption rates
   - Identify optimization opportunities
   - Version complexity metrics

5. **Cache & Response** (Steps 16-19)
   - Cache results for 30 minutes
   - Return detailed template analytics
   - Render visualizations

### Real-time Dashboard Flow

1. **WebSocket Connection** (Step 1)
   - Admin opens dashboard
   - Establish WebSocket connection
   - API Gateway validates JWT token before connection is established

2. **Channel Subscription** (Steps 2-4)
   - Client subscribes to analytics channel
   - Server subscribes to event bus
   - Listen for document and template events

3. **Initial Data Load** (Steps 5-10)
   - Fetch current statistics via REST API
   - Render initial dashboard state
   - Display to admin

4. **Real-time Event Processing** (Steps 11-22)
   - Document uploaded/deleted/updated events published
   - Event bus forwards to WebSocket server
   - Server calculates incremental updates
   - Push updates to connected clients
   - Client updates dashboard in real-time
   - Smooth animations and notifications

## API Endpoint Details

### Get Document Statistics

```
GET /api/v1/analytics/documents/stats?date_from=2024-01-01&date_to=2024-11-01&group_by=type
Authorization: Bearer <token>
```

**Query Parameters:**
- `customer_id` - Filter by specific customer (optional)
- `date_from` - Start date (ISO 8601)
- `date_to` - End date (ISO 8601)
- `group_by` - Group results by: type, category, customer, month

**Success Response (200 OK):**
```json
{
  "total_documents": 1245678,
  "customer_specific_documents": 1245100,
  "shared_documents": 578,
  "by_type": [
    {
      "document_type": "ACCOUNT_STATEMENT",
      "count": 456789,
      "percentage": 36.7
    },
    {
      "document_type": "LOAN_APPLICATION",
      "count": 123456,
      "percentage": 9.9
    }
  ],
  "by_category": [
    {
      "document_category": "BANKING",
      "count": 678901,
      "percentage": 54.5
    },
    {
      "document_category": "LENDING",
      "count": 234567,
      "percentage": 18.8
    }
  ],
  "storage_usage": {
    "total_bytes": 5368709120,
    "total_gb": 5.0,
    "avg_bytes": 4310,
    "max_bytes": 52428800
  },
  "recent_activity": {
    "documents_uploaded_today": 1245,
    "documents_uploaded_this_week": 8901,
    "documents_uploaded_this_month": 34567
  },
  "query_time_ms": 387,
  "cached": false,
  "generated_at": "2024-11-01T12:00:00Z"
}
```

### Get Template Usage Statistics

```
GET /api/v1/analytics/templates/usage
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "total_templates": 400,
  "active_templates": 385,
  "deprecated_templates": 15,
  "draft_templates": 0,
  "templates_with_documents": [
    {
      "template_code": "ACCOUNT_STATEMENT",
      "template_name": "Monthly Account Statement",
      "version_number": 2,
      "document_count": 456789,
      "status": "active",
      "usage_percentage": 36.7
    },
    {
      "template_code": "LOAN_APPLICATION",
      "template_name": "Loan Application Form",
      "version_number": 3,
      "document_count": 123456,
      "status": "active",
      "usage_percentage": 9.9
    }
  ],
  "unused_templates": [
    {
      "template_code": "UNUSED_FORM",
      "template_name": "Old Application Form",
      "status": "active",
      "created_at": "2023-06-15T10:00:00Z"
    }
  ],
  "version_statistics": {
    "avg_versions_per_template": 2.3,
    "max_versions": 8,
    "templates_with_multiple_versions": 45
  },
  "query_time_ms": 245,
  "cached": true,
  "generated_at": "2024-11-01T12:00:00Z"
}
```

## Performance Optimizations

### Query Optimization
```sql
-- Materialized view for fast analytics
CREATE MATERIALIZED VIEW document_statistics AS
SELECT
  DATE_TRUNC('day', uploaded_at) as date,
  document_type,
  document_category,
  COUNT(*) as count,
  SUM(file_size_bytes) as total_size
FROM documents
WHERE deleted_at IS NULL
GROUP BY date, document_type, document_category;

-- Refresh periodically (every hour)
REFRESH MATERIALIZED VIEW CONCURRENTLY document_statistics;

-- Index for fast lookups
CREATE INDEX idx_document_statistics_date
ON document_statistics(date, document_type);
```

### Caching Strategy
- **Document Stats Cache**: 15-minute TTL
- **Template Stats Cache**: 30-minute TTL
- **Dashboard Aggregates**: 5-minute TTL
- **Real-time Counters**: Redis incrementers (no TTL)

### Database Indexes
```sql
-- Analytics queries
CREATE INDEX idx_documents_uploaded_at
ON documents(uploaded_at DESC)
WHERE deleted_at IS NULL;

CREATE INDEX idx_documents_type_date
ON documents(document_type, uploaded_at)
WHERE deleted_at IS NULL;

CREATE INDEX idx_documents_category
ON documents(document_category, uploaded_at)
WHERE deleted_at IS NULL;

-- Template analytics
CREATE INDEX idx_documents_template_id
ON documents(template_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_templates_status
ON templates(status, created_at);
```

### Query Performance Targets
- **Document Statistics**: <500ms (first request), <10ms (cached)
- **Template Usage**: <300ms (first request), <5ms (cached)
- **Real-time Updates**: <50ms latency
- **Dashboard Load**: <1 second (total page load)

## Visualization Examples

### Dashboard Components

1. **Document Type Distribution** (Pie Chart)
   - Visualize document types by count
   - Interactive segments
   - Show percentages

2. **Upload Activity Timeline** (Line Chart)
   - Documents uploaded over time
   - Daily, weekly, monthly views
   - Trend lines

3. **Storage Usage** (Gauge/Progress)
   - Current storage vs capacity
   - Growth rate
   - Cost projections

4. **Template Usage Heatmap**
   - Template activity matrix
   - Color-coded usage intensity
   - Identify popular templates

5. **Real-time Counter**
   - Live document count
   - Animated increments
   - Recent activity feed

## Error Scenarios

| Scenario | HTTP Status | Error Code | Action |
|----------|-------------|------------|--------|
| Authentication (401 Unauthorized) | 401 | UNAUTHORIZED | Handled by API Gateway; not shown in this diagram |
| Insufficient permissions | 403 | INSUFFICIENT_PERMISSIONS | Need analytics:read role |
| Invalid date range | 400 | INVALID_DATE_RANGE | Check date format |
| Query timeout | 504 | QUERY_TIMEOUT | Simplify query or use cache |
| WebSocket auth failed | 403 | WS_AUTH_FAILED | Invalid token (checked at gateway) |

## Best Practices

1. **Use Caching Aggressively**
   - Analytics queries are expensive
   - Cache results for appropriate TTL
   - Invalidate on major data changes

2. **Implement Materialized Views**
   - Pre-compute common aggregations
   - Refresh periodically
   - Much faster than live queries

3. **Real-time for Critical Metrics**
   - Use WebSocket for live dashboards
   - Incremental updates, not full refreshes
   - Reduces server load

4. **Optimize Query Complexity**
   - Limit date ranges for better performance
   - Use pagination for large result sets
   - Consider data sampling for approximate results

5. **Monitor Query Performance**
   - Log slow queries (> 1 second)
   - Alert on degraded performance
   - Optimize indexes based on usage patterns

6. **Provide Data Export**
   - Allow CSV/Excel export for offline analysis
   - Generate reports asynchronously
   - Email large reports instead of inline

## Advanced Analytics Features

### Predictive Analytics
- **Storage Forecasting**: Predict when storage limits will be reached
- **Usage Trends**: Identify growing/declining document types
- **Capacity Planning**: Recommend infrastructure scaling

### Custom Reports
- **Scheduled Reports**: Daily/weekly email summaries
- **Custom Dashboards**: User-defined metrics and visualizations
- **Data Export**: CSV, JSON, Excel formats

### Compliance Reporting
- **Document Retention**: Track documents approaching retention expiry
- **Access Audits**: Who accessed what documents
- **Regulatory Reports**: Compliance-specific analytics
