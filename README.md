# AEK PDF (เอก พีดีเอฟ)

> แอพพลิเคชัน Android ระดับมืออาชีพสำหรับแปลงภาพสแกนเอกสารเป็นไฟล์ PDF ไร้ลายน้ำ (No Watermark) ฟรี 100% ประมวลผลแบบ Offline ในเครื่องโดยสมบูรณ์ และออกแบบ UX ให้ใช้งานง่ายสำหรับผู้ใหญ่/คุณพ่อ

[![Latest Release](https://img.shields.io/github/v/release/Chitsanuphong628/aek-pdf?color=007AFF&label=Release&logo=android)](https://github.com/Chitsanuphong628/aek-pdf/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-34C759.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android%208.0%2B-5856D6.svg)](https://developer.android.com)

ดาวน์โหลดไฟล์ติดตั้งเวอร์ชันแรก: [**aek-pdf-v1.0.0.apk (12.6 MB)**](https://github.com/Chitsanuphong628/aek-pdf/releases/download/v1.0.0/aek-pdf-v1.0.0.apk)

---

## จุดเด่นของแอพพลิเคชัน (Key Features)

1. **ถ่ายรัวต่อเนื่อง (Rapid Continuous Multi-Page Camera)**: ถ่ายเอกสารหลายหน้าติดต่อกันได้อย่างรวดเร็ว ไม่ต้องตัดเข้าหน้าตรวจทานทีละหน้า ช่องมองภาพเปิดต่อเนื่องพร้อมตัวนับจำนวนหน้าและพรีวิวสด กดเสร็จสิ้นเพื่อเข้าสู่หน้าจัดเรียงได้ทันที
2. **ฟรี 100% & ไร้ลายน้ำ (No Watermarks)**: เอกสารสะอาด ปราศจากลายน้ำหรือโลโก้ของผู้พัฒนา
3. **ทำงานในเครื่อง 100% (Offline & Privacy-First)**: รูปภาพและเอกสารทั้งหมดถูกประมวลผลภายในอุปกรณ์ ไม่มีการอัพโหลดขึ้น Cloud Server ปลอดภัยจากข้อมูลส่วนบุคคลรั่วไหล
4. **ระบบจัดการหน่วยความจำปลอดภัยสูง (OOM-Safe Streaming)**: ป้องกันปัญหาแอพเด้ง (OutOfMemoryError) จากการนำเข้ารูปถ่ายเอกสารสแกนความละเอียดสูงหลายสิบภาพ ด้วยระบบประมวลผลแบบ Single-Page Bitmap Streaming & Subsampling
5. **ออกแบบ UX เพื่อคุณพ่อ (Elderly & Accessible UI)**:
   - สไตล์ Apple Minimalist คลีนตา ไร้สิ่งรบกวน
   - สไลด์ซ้ายเพื่อลบ (Swipe to Delete) พร้อมแอนิเมชันคืนรูปและกดยกเลิกได้
   - แดร็กแอนด์ดรอป (Drag & Drop) เพื่อสลับจัดเรียงลำดับหน้า
   - พรีวิวเอกสารจริงทันทีก่อนส่งออก พร้อมปุ่มกดส่งเข้า **LINE**, **Google Drive**, หรือ **เปิดดู** ได้ใน 1 คลิก
6. **บันทึกประวัติเอกสาร (Local History)**: จดจำไฟล์ PDF ที่เคยสร้างไว้ในเครื่องผ่าน Room Database เปิดดูย้อนหลังหรือแชร์ซ้ำได้ตลอดเวลา
7. **สแกนตัดขอบอัตโนมัติ (Google ML Kit)**: ตรวจจับขอบกระดาษ ปรับมุมเอียง และฟิลเตอร์ขาวดำเอกสารอัตโนมัติ

---

## โครงสร้างสถาปัตยกรรม (Architecture)

```
com.aekpdf.app/
├── AekPdfApplication.kt               # Application Class & Singletons
├── MainActivity.kt                    # Entry Activity with Navigation Graph
├── core/
│   ├── model/                         # Data Models (DocumentPage, PageFormat, ExportQuality, Config)
│   ├── pdf/                           # Native PDF Engine
│   │   ├── StreamingPdfCreator.kt     # OOM-Safe Streaming PDF Generation
│   │   └── ImageProcessor.kt          # Fast EXIF-aware Bitmap Decoding & Document Filters
│   ├── storage/                       # Scoped Storage & FileProvider Share Intents
│   │   └── PdfStorageManager.kt
│   ├── database/                      # Room Local Database (History)
│   │   ├── AppDatabase.kt
│   │   ├── PdfHistoryDao.kt
│   │   └── PdfHistoryEntity.kt
│   └── ui/theme/                      # Material 3 Accessible Colors & Theme
└── features/
    ├── home/                          # หน้าหลัก Hub รวม 4 เครื่องมือจัดการเอกสาร
    ├── camera/                        # โหมดกล้องถ่ายรัวต่อเนื่อง (CameraX Rapid Continuous Mode)
    ├── imagetopdf/                    # จัดหน้า ลากสลับลำดับ สไลด์ลบ และสร้าง PDF
    ├── viewer/                        # หน้าพรีวิวเอกสารด้วย Native PdfRenderer & แชร์ด่วน
    └── history/                       # หน้ารวมประวัติไฟล์ PDF ที่เคยสร้าง
```

---

## เทคโนโลยีที่ใช้ (Tech Stack)

- **Language**: Kotlin 2.0.21
- **UI Toolkit**: Jetpack Compose & Material 3 (Apple Minimalist Design System)
- **Camera Engine**: AndroidX CameraX 1.4.0 (Latency-minimized continuous shooting)
- **Document Scanner**: Google ML Kit Document Scanner
- **Image Pipeline**: Coil 2.7.0 & Android Native Bitmap Subsampling
- **PDF Engine**: Android Native `PdfDocument` & `PdfRenderer`
- **Database**: AndroidX Room 2.6.1 + KSP
- **Asynchronous**: Kotlin Coroutines & StateFlow
- **Minimum SDK**: Android 8.0 Oreo (API 26)
- **Target SDK**: Android 15 (API 35)

---

## การคอมไพล์และติดตั้ง (Build & Run)

```bash
# คอมไพล์และรัน Unit Tests
./gradlew test

# สร้างไฟล์ติดตั้ง APK (Debug)
./gradlew assembleDebug

# ไฟล์ APK จะถูกสร้างไว้ที่:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## Roadmap ฟีเจอร์ในอนาคต (Extensibility)

1. **รวมหลายไฟล์ PDF (PDF Merge)**: นำเข้าไฟล์ PDF หลายไฟล์แล้วรวมเป็นไฟล์เดียว
2. **ย่อขนาดไฟล์ PDF (PDF Compress)**: ปรับลดความละเอียดเพื่อส่งอีเมลหรือแนบไฟล์ผ่านช่องทางจำกัดขนาด
3. **สแกนเอกสารด้วยกล้อง (Camera Document Scanner)**: ตรวจจับขอบกระดาษและปรับมุมภาพอัตโนมัติด้วย Google ML Kit

---

## สัญญาอนุญาต (License)

โครงการนี้เผยแพร่ภายใต้สัญญาอนุญาตแบบโอเพนซอร์ส [MIT License](LICENSE) สามารถนำไปใช้งาน ปรับปรุง และพัฒนาต่อยอดได้อย่างอิสระ

สำหรับ AI Agents และนักพัฒนาที่ต้องการทำความเข้าใจโครงสร้างเชิงลึก สามารถอ่านคู่มือเพิ่มเติมได้ที่ [README_FOR_AI.md](README_FOR_AI.md)
