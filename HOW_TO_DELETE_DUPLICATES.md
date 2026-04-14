# How to Delete Duplicate Projects

## Quick Start (Easiest Method)

### Step 1: Run the Interactive Script

Open PowerShell in the backend folder and run:

```powershell
.\delete-duplicate-projects.ps1
```

### Step 2: Follow the Prompts

The script will:
1. Show you all projects with their IDs
2. Ask which ones to delete
3. Confirm before deleting
4. Show results

### Example Session:

```
========================================
  Project Deletion Tool
========================================

Fetching projects from server...
Found 13 projects

Current Projects:
----------------------------------------
1. [3SINDUSTRIALCOMPLEX] 3S Industrial Complex
2. [AS] AS
3. [DEFAULT] Default Project
4. [EMADALBAHARCHALET] Emad Al Bahar Chalet
5. [KIA-3SFACILITY] KIA- 3S Facility
6. [MANA] Mana
7. [MERCEDES-AHMA] Mercedes-Ahma...
8. [MERCEDES-SHUW] Mercedes-Shu...
9. [MERCEDES-SHUWAIKH] Mercedes-Shuwaikh
10. [KCC] KCC
11. [SHAMAAL-MEDICA] Shamaal Medica...
12. [TAJAL-MULLA] Tajal Mulla
13. [TAJAL-MULLA-C] Tajal Mulla - C...
----------------------------------------

Enter the PROJECT IDs to delete (comma-separated):
Example: DEFAULT,MANA,MERCEDES-SHUWAIKH-2

Project IDs to delete (or 'cancel' to exit): DEFAULT,MANA,MERCEDES-AHMA

You are about to delete the following projects:
  - [DEFAULT] Default Project
  - [MANA] Mana
  - [MERCEDES-AHMA] Mercedes-Ahma...

Are you sure? (yes/no): yes

Deleting projects...
Deleting: DEFAULT ... ✅ SUCCESS
Deleting: MANA ... ✅ SUCCESS
Deleting: MERCEDES-AHMA ... ✅ SUCCESS

========================================
Summary:
  ✅ Deleted: 3
  ❌ Failed: 0
========================================
```

---

## Manual Method (Alternative)

If the script doesn't work, you can delete manually:

### Using Browser:

1. Open: `http://localhost:8080/api/projects`
2. Find the project ID you want to delete
3. Open: `http://localhost:8080/api/projects/{PROJECT_ID}`
4. The project will be deleted

### Using PowerShell Commands:

```powershell
# Delete Default Project
Invoke-RestMethod -Uri "http://localhost:8080/api/projects/DEFAULT" -Method Delete

# Delete Mana
Invoke-RestMethod -Uri "http://localhost:8080/api/projects/MANA" -Method Delete

# Delete Mercedes duplicate (replace with actual ID)
Invoke-RestMethod -Uri "http://localhost:8080/api/projects/MERCEDES-AHMA" -Method Delete
```

---

## Important Notes

⚠️ **Before Deleting**:
- Make sure the backend server is running
- Identify the EXACT project ID (case-sensitive)
- Keep at least ONE version of each project

⚠️ **Cannot Delete If**:
- Project has action sheets assigned to it
- The system will prevent deletion to protect data

✅ **Safe to Delete**:
- "Default Project" - if you don't use it
- "Mana" - if it's a duplicate
- Mercedes variations - keep only the correct one

---

## Identifying Mercedes Duplicates

You mentioned Mercedes-Shuwaikh has duplicates. Look for:
- `MERCEDES-SHUWAIKH` ✅ (keep this one - correct format)
- `MERCEDES-AHMA` ❌ (delete - truncated)
- `MERCEDES-SHUW` ❌ (delete - truncated)
- `MERCEDES-SHUWAIKH-2` ❌ (delete - duplicate)

**Rule**: Keep the one with the full, correct name. Delete truncated or numbered versions.

---

## Troubleshooting

### Script Error: "Cannot connect to server"
- Make sure backend is running: `.\setup-and-run-server.bat`
- Check URL: `http://localhost:8080/api/projects`

### Error: "Failed to delete"
- Project might have action sheets assigned
- Check if project is in use
- Try deleting action sheets first

### Project Still Shows After Deletion
- Refresh your browser (Ctrl + F5)
- Clear browser cache
- Restart the backend server

---

## Need Help?

If you're unsure which projects to delete:

1. Run this command to see all projects:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/api/projects" | ConvertTo-Json
   ```

2. Send me the output and I'll tell you exactly which ones to delete.
