# Delete Duplicate Projects - API Guide

## Step 1: View All Projects

Open your browser and go to:
```
http://localhost:8080/api/projects
```

This will show you all projects with their IDs and names in JSON format.

---

## Step 2: Identify Duplicates

Look for these duplicate projects in the list:
- **Default Project** - Delete if unwanted
- **Mana** - Check if there are multiple entries
- **Mercedes-Shuwaikh** - Look for variations like:
  - "Mercedes Shuwaikh"
  - "Mercedes- Shuwaikh" (with space after dash)
  - "Mercedes-Shuwaikh" (no space)
  - "Mercedes Ahm..." (truncated)
  - "Mercedes-Shu..." (truncated)

**IMPORTANT**: Write down the exact `id` of each project you want to DELETE.

---

## Step 3: Delete Projects Using API

### Option A: Using Browser (Simple)

For each project you want to delete, open this URL in your browser:
```
http://localhost:8080/api/projects/{PROJECT_ID}
```

Replace `{PROJECT_ID}` with the actual project ID.

**Example**:
```
http://localhost:8080/api/projects/DEFAULT
http://localhost:8080/api/projects/MANA
```

### Option B: Using PowerShell (Recommended)

Open PowerShell and run these commands:

```powershell
# Delete "Default Project"
Invoke-RestMethod -Uri "http://localhost:8080/api/projects/DEFAULT" -Method Delete

# Delete "Mana" (if duplicate)
Invoke-RestMethod -Uri "http://localhost:8080/api/projects/MANA" -Method Delete

# Delete Mercedes-Shuwaikh duplicate (replace with actual ID)
Invoke-RestMethod -Uri "http://localhost:8080/api/projects/MERCEDES-SHUWAIKH-DUPLICATE-ID" -Method Delete
```

### Option C: Using curl (Alternative)

```bash
# Delete "Default Project"
curl -X DELETE http://localhost:8080/api/projects/DEFAULT

# Delete "Mana"
curl -X DELETE http://localhost:8080/api/projects/MANA

# Delete Mercedes-Shuwaikh duplicate
curl -X DELETE http://localhost:8080/api/projects/MERCEDES-SHUWAIKH-DUPLICATE-ID
```

---

## Step 4: Verify Deletion

Refresh the projects page in your browser:
```
http://localhost:8080/api/projects
```

Or refresh the frontend projects list.

---

## SAFETY NOTES

⚠️ **BEFORE DELETING**:
1. Make sure you have the correct project ID
2. Check if any action sheets are assigned to this project
3. Keep at least ONE version of each project (delete only duplicates)

⚠️ **CANNOT DELETE IF**:
- Project has active action sheets assigned to it
- The backend will prevent deletion to protect data integrity

---

## Example: Finding Mercedes-Shuwaikh Duplicates

If you see multiple Mercedes projects in the list:

```json
[
  {
    "id": "MERCEDES-SHUWAIKH",
    "name": "Mercedes-Shuwaikh",
    "createdDate": "2026-01-15T10:30:00"
  },
  {
    "id": "MERCEDES-SHUWAIKH-2",
    "name": "Mercedes- Shuwaikh",
    "createdDate": "2026-02-20T14:45:00"
  },
  {
    "id": "MERCEDESSHUW",
    "name": "Mercedes Ahm...",
    "createdDate": "2026-03-10T09:15:00"
  }
]
```

**Decision**:
- Keep: `MERCEDES-SHUWAIKH` (oldest, correct format)
- Delete: `MERCEDES-SHUWAIKH-2` (duplicate with space)
- Delete: `MERCEDESSHUW` (truncated/incorrect)

---

## Quick Delete Script

Once you identify the IDs to delete, create a PowerShell script:

```powershell
# delete-duplicates.ps1
$baseUrl = "http://localhost:8080/api/projects"

# List of project IDs to delete
$projectsToDelete = @(
    "DEFAULT",
    "MANA",
    "MERCEDES-SHUWAIKH-2"
)

foreach ($projectId in $projectsToDelete) {
    Write-Host "Deleting project: $projectId"
    try {
        Invoke-RestMethod -Uri "$baseUrl/$projectId" -Method Delete
        Write-Host "✅ Deleted: $projectId" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to delete: $projectId - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`nDone! Refresh the projects page to verify."
```

Save as `delete-duplicates.ps1` and run:
```powershell
.\delete-duplicates.ps1
```

---

## Need Help?

If you're unsure which projects to delete, send me the output of:
```
http://localhost:8080/api/projects
```

And I'll help you identify the exact duplicates to remove.
