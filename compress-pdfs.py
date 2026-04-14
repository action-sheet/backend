#!/usr/bin/env python3
"""
PDF Compression Script
Compresses all PDFs in E:/Action Sheet System/data/ recursively
Creates backups before compression
"""

import os
import sys
import shutil
from pathlib import Path
from datetime import datetime

try:
    from PyPDF2 import PdfReader, PdfWriter
except ImportError:
    print("ERROR: PyPDF2 not installed")
    print("Install it with: pip install PyPDF2")
    sys.exit(1)

# Configuration
DATA_DIR = Path("E:/Action Sheet System/data")
BACKUP_DIR = Path("E:/Action Sheet System/data_backup_" + datetime.now().strftime("%Y%m%d_%H%M%S"))
TARGET_SIZE_MB = 10  # Target max size in MB
MIN_COMPRESSION_RATIO = 0.8  # Only keep compressed if at least 20% smaller

def get_size_mb(file_path):
    """Get file size in MB"""
    return os.path.getsize(file_path) / (1024 * 1024)

def compress_pdf(input_path, output_path):
    """Compress a single PDF file"""
    try:
        reader = PdfReader(input_path)
        writer = PdfWriter()
        
        # Copy all pages
        for page in reader.pages:
            # Compress images and content
            page.compress_content_streams()
            writer.add_page(page)
        
        # Write compressed PDF
        with open(output_path, 'wb') as output_file:
            writer.write(output_file)
        
        return True
    except Exception as e:
        print(f"  ❌ Error compressing: {e}")
        return False

def process_pdfs(data_dir, backup_dir):
    """Process all PDFs in the data directory"""
    
    if not data_dir.exists():
        print(f"❌ Data directory not found: {data_dir}")
        return
    
    # Create backup directory
    print(f"📁 Creating backup directory: {backup_dir}")
    backup_dir.mkdir(parents=True, exist_ok=True)
    
    # Find all PDFs
    pdf_files = list(data_dir.rglob("*.pdf"))
    total_pdfs = len(pdf_files)
    
    if total_pdfs == 0:
        print("⚠️  No PDF files found")
        return
    
    print(f"\n📄 Found {total_pdfs} PDF files")
    print("=" * 80)
    
    compressed_count = 0
    skipped_count = 0
    error_count = 0
    total_saved_mb = 0
    
    for idx, pdf_path in enumerate(pdf_files, 1):
        original_size_mb = get_size_mb(pdf_path)
        
        print(f"\n[{idx}/{total_pdfs}] {pdf_path.name}")
        print(f"  Original size: {original_size_mb:.2f} MB")
        
        # Skip if already small enough
        if original_size_mb < TARGET_SIZE_MB:
            print(f"  ✓ Already small enough (< {TARGET_SIZE_MB} MB), skipping")
            skipped_count += 1
            continue
        
        # Create backup
        relative_path = pdf_path.relative_to(data_dir)
        backup_path = backup_dir / relative_path
        backup_path.parent.mkdir(parents=True, exist_ok=True)
        
        try:
            shutil.copy2(pdf_path, backup_path)
            print(f"  💾 Backed up to: {backup_path}")
        except Exception as e:
            print(f"  ❌ Backup failed: {e}")
            error_count += 1
            continue
        
        # Compress
        temp_path = pdf_path.with_suffix('.pdf.tmp')
        print(f"  🔄 Compressing...")
        
        if compress_pdf(pdf_path, temp_path):
            compressed_size_mb = get_size_mb(temp_path)
            compression_ratio = compressed_size_mb / original_size_mb
            saved_mb = original_size_mb - compressed_size_mb
            
            print(f"  Compressed size: {compressed_size_mb:.2f} MB")
            print(f"  Saved: {saved_mb:.2f} MB ({(1-compression_ratio)*100:.1f}% reduction)")
            
            # Only use compressed version if significantly smaller
            if compression_ratio < MIN_COMPRESSION_RATIO:
                # Replace original with compressed
                os.replace(temp_path, pdf_path)
                print(f"  ✅ Compression successful")
                compressed_count += 1
                total_saved_mb += saved_mb
            else:
                # Compression didn't help much, keep original
                os.remove(temp_path)
                print(f"  ⚠️  Compression ratio too high, keeping original")
                skipped_count += 1
        else:
            error_count += 1
            if temp_path.exists():
                os.remove(temp_path)
    
    # Summary
    print("\n" + "=" * 80)
    print("📊 COMPRESSION SUMMARY")
    print("=" * 80)
    print(f"Total PDFs found:     {total_pdfs}")
    print(f"Compressed:           {compressed_count}")
    print(f"Skipped:              {skipped_count}")
    print(f"Errors:               {error_count}")
    print(f"Total space saved:    {total_saved_mb:.2f} MB")
    print(f"\nBackup location:      {backup_dir}")
    print("=" * 80)

if __name__ == "__main__":
    print("=" * 80)
    print("PDF COMPRESSION TOOL")
    print("=" * 80)
    print(f"Data directory: {DATA_DIR}")
    print(f"Target size: < {TARGET_SIZE_MB} MB per PDF")
    print()
    
    response = input("⚠️  This will compress all large PDFs. Continue? (yes/no): ")
    if response.lower() != 'yes':
        print("Cancelled.")
        sys.exit(0)
    
    process_pdfs(DATA_DIR, BACKUP_DIR)
    print("\n✅ Done!")
