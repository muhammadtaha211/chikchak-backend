// Data Initialization
package com.chikchak.config;

import com.chikchak.model.JobType;
import com.chikchak.model.ServiceType;
import com.chikchak.model.User;
import com.chikchak.model.UserRole;
import com.chikchak.repository.JobTypeRepository;
import com.chikchak.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    // Standardized visit fee for all technicians (MOVERS get ₪0)
    public static final BigDecimal STANDARD_DIAGNOSIS_FEE = new BigDecimal("80");
    public static final BigDecimal MOVERS_DIAGNOSIS_FEE = BigDecimal.ZERO;

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               JobTypeRepository jobTypeRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default admin user
            if (!userRepository.existsByEmail("admin@chikchak.com")) {
                User admin = User.builder()
                        .email("admin@chikchak.com")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("ChikChak Admin")
                        .role(UserRole.ADMIN)
                        .preferredLanguage("he")
                        .build();
                userRepository.save(admin);
                log.info("Default admin user created");
            }

            // Seed platform-set job type catalog (EN/HE/AR + min/max price)
            if (jobTypeRepository.count() == 0) {
                seedJobTypes(jobTypeRepository);
                log.info("Seeded {} job types", JOB_TYPE_CATALOG.size());
            }
        };
    }

    private void seedJobTypes(JobTypeRepository jobTypeRepository) {
        for (SeedJob job : JOB_TYPE_CATALOG) {
            JobType jobType = JobType.builder()
                    .name(job.name())
                    .nameHe(job.nameHe())
                    .nameAr(job.nameAr())
                    .serviceType(job.serviceType())
                    .minPrice(BigDecimal.valueOf(job.min()))
                    .maxPrice(BigDecimal.valueOf(job.max()))
                    .build();
            jobTypeRepository.save(jobType);
        }
    }

    private record SeedJob(String name, String nameHe, String nameAr,
                           ServiceType serviceType, int min, int max) {}

    private static final List<SeedJob> JOB_TYPE_CATALOG = List.of(
            // ── AC_REPAIR (21) ───────────────────────────────────────────────
            new SeedJob("AC technician visit", "ביקור טכנאי מזגנים", "زيارة فني تكييف", ServiceType.AC_REPAIR, 250, 350),
            new SeedJob("AC gas refill", "מילוי גז למזגן", "تعبئة غاز التكييف", ServiceType.AC_REPAIR, 500, 1600),
            new SeedJob("Gas leak detection and repair", "איתור ותיקון דליפת גז", "كشف وإصلاح تسرب الغاز", ServiceType.AC_REPAIR, 500, 2100),
            new SeedJob("Compressor replacement", "החלפת מדחס", "استبدال الضاغط", ServiceType.AC_REPAIR, 3000, 4500),
            new SeedJob("Capacitor replacement", "החלפת קבל", "استبدال المكثف", ServiceType.AC_REPAIR, 400, 550),
            new SeedJob("Temperature sensor replacement", "החלפת חיישן טמפרטורה", "استبدال حساس درجة الحرارة", ServiceType.AC_REPAIR, 380, 580),
            new SeedJob("Indoor unit fan motor replacement", "החלפת מנוע מאוורר ביחידה הפנימית", "استبدال محرك مروحة الوحدة الداخلية", ServiceType.AC_REPAIR, 600, 1500),
            new SeedJob("Outdoor unit fan motor replacement", "החלפת מנוע מאוורר ביחידה החיצונית", "استبدال محرك مروحة الوحدة الخارجية", ServiceType.AC_REPAIR, 600, 1300),
            new SeedJob("Control board replacement", "החלפת כרטיס פיקוד", "استبدال لوحة التحكم", ServiceType.AC_REPAIR, 600, 5500),
            new SeedJob("Heat pump replacement", "החלפת משאבת חימום", "استبدال مضخة التدفئة", ServiceType.AC_REPAIR, 800, 1200),
            new SeedJob("Heat pump coil replacement", "החלפת סליל משאבת חימום", "استبدال ملف مضخة التدفئة", ServiceType.AC_REPAIR, 450, 650),
            new SeedJob("Blower / fan booster replacement", "החלפת מאיץ", "استبدال المسرّع", ServiceType.AC_REPAIR, 450, 800),
            new SeedJob("AC drain pan repair", "תיקון אמבטיית ניקוז במזגן", "إصلاح حوض تصريف التكييف", ServiceType.AC_REPAIR, 350, 550),
            new SeedJob("Drain pipe replacement", "החלפת צינור ניקוז", "استبدال أنبوب التصريف", ServiceType.AC_REPAIR, 400, 600),
            new SeedJob("Display panel replacement", "החלפת עינית", "استبدال شاشة العرض", ServiceType.AC_REPAIR, 400, 800),
            new SeedJob("Short circuit repair", "תיקון קצר", "إصلاح ماس كهربائي", ServiceType.AC_REPAIR, 350, 550),
            new SeedJob("Remote control replacement", "החלפת שלט", "استبدال جهاز التحكم عن بعد", ServiceType.AC_REPAIR, 300, 450),
            new SeedJob("Wall-mounted AC installation", "התקנת מזגן עילי", "تركيب مكيف جداري", ServiceType.AC_REPAIR, 800, 1800),
            new SeedJob("Mini-central AC installation", "התקנת מזגן מיני מרכזי", "تركيب مكيف مركزي صغير", ServiceType.AC_REPAIR, 4000, 5500),
            new SeedJob("AC cleaning", "ניקוי מזגן", "تنظيف المكيف", ServiceType.AC_REPAIR, 250, 1100),
            new SeedJob("AC leak repair", "תיקון נזילה מהמזגן", "إصلاح تسرب من المكيف", ServiceType.AC_REPAIR, 250, 2000),

            // ── MOVERS (14) ──────────────────────────────────────────────────
            new SeedJob("1-room apartment moving", "הובלת דירת חדר 1", "نقل شقة غرفة واحدة", ServiceType.MOVERS, 1300, 1600),
            new SeedJob("2-room apartment moving", "הובלת דירת 2 חדרים", "نقل شقة غرفتين", ServiceType.MOVERS, 1700, 2300),
            new SeedJob("3-room apartment moving", "הובלת דירת 3 חדרים", "نقل شقة 3 غرف", ServiceType.MOVERS, 2600, 3300),
            new SeedJob("4-room apartment moving", "הובלת דירת 4 חדרים", "نقل شقة 4 غرف", ServiceType.MOVERS, 3400, 4500),
            new SeedJob("5-room apartment moving", "הובלת דירת 5 חדרים", "نقل شقة 5 غرف", ServiceType.MOVERS, 4500, 6500),
            new SeedJob("Single item moving", "הובלת פריט בודד", "نقل قطعة واحدة", ServiceType.MOVERS, 400, 650),
            new SeedJob("Office moving up to 3 rooms", "הובלת משרד עד 3 חדרים", "نقل مكتب حتى 3 غرف", ServiceType.MOVERS, 1500, 2700),
            new SeedJob("4-room office moving", "הובלת משרד 4 חדרים", "نقل مكتب 4 غرف", ServiceType.MOVERS, 2300, 3700),
            new SeedJob("5-room office moving", "הובלת משרד 5 חדרים", "نقل مكتب 5 غرف", ServiceType.MOVERS, 3000, 4500),
            new SeedJob("6-room office moving", "הובלת משרד 6 חדרים", "نقل مكتب 6 غرف", ServiceType.MOVERS, 4700, 6000),
            new SeedJob("Lifting crane rental", "השכרת מנוף הרמה", "تأجير رافعة", ServiceType.MOVERS, 350, 500),
            new SeedJob("Packing services", "שירותי אריזה", "خدمات التغليف", ServiceType.MOVERS, 35, 45),
            new SeedJob("Piano moving", "הובלת פסנתר", "نقل بيانو", ServiceType.MOVERS, 1000, 1200),
            new SeedJob("Storage services", "שירותי אחסון", "خدمات التخزين", ServiceType.MOVERS, 600, 1200),

            // ── PLUMBING (35) ────────────────────────────────────────────────
            new SeedJob("Plumber visit", "ביקור אינסטלטור", "زيارة سباك", ServiceType.PLUMBING, 250, 350),
            new SeedJob("Toilet unclogging", "פתיחת סתימה באסלה", "فتح انسداد المرحاض", ServiceType.PLUMBING, 350, 500),
            new SeedJob("Sink unclogging", "פתיחת סתימה בכיור", "فتح انسداد الحوض", ServiceType.PLUMBING, 350, 550),
            new SeedJob("Bathtub unclogging", "פתיחת סתימה באמבטיה", "فتح انسداد البانيو", ServiceType.PLUMBING, 350, 450),
            new SeedJob("Drain channel unclogging", "פתיחת סתימה בתעלת הניקוז", "فتح انسداد قناة التصريف", ServiceType.PLUMBING, 380, 450),
            new SeedJob("Gutter unclogging", "פתיחת סתימה במרזב", "فتح انسداد المزراب", ServiceType.PLUMBING, 400, 585),
            new SeedJob("Thermal camera leak detection", "איתור נזילה במצלמה תרמית", "كشف تسرب بالكاميرا الحرارية", ServiceType.PLUMBING, 1170, 1755),
            new SeedJob("Gas sensor leak detection", "איתור נזילה בחיישן גז", "كشف تسرب بمستشعر الغاز", ServiceType.PLUMBING, 1800, 2100),
            new SeedJob("Balloon test leak detection", "איתור נזילה באמצעות בדיקת בלון", "كشف تسرب بفحص البالون", ServiceType.PLUMBING, 800, 1000),
            new SeedJob("Fiber optic camera leak detection (wastewater & sewage)", "איתור נזילה במצלמת סיב אופטי למערכות מי קולחין וביוב", "كشف تسرب بكاميرا الألياف البصرية لأنظمة الصرف", ServiceType.PLUMBING, 2100, 2500),
            new SeedJob("Toilet leak repair", "תיקון נזילה באסלה", "إصلاح تسرب المرحاض", ServiceType.PLUMBING, 350, 550),
            new SeedJob("Faucet leak repair", "תיקון נזילה בברז", "إصلاح تسرب الصنبور", ServiceType.PLUMBING, 300, 450),
            new SeedJob("Cistern leak repair", "תיקון נזילה בניאגרה", "إصلاح تسرب خزان المرحاض", ServiceType.PLUMBING, 340, 450),
            new SeedJob("Sink trap leak repair", "תיקון נזילה בסיפון מתחת לכיור", "إصلاح تسرب سيفون الحوض", ServiceType.PLUMBING, 300, 450),
            new SeedJob("Cistern flush mechanism replacement", "החלפת מנגנון הדחה בניאגרה", "استبدال آلية الطرد في خزان المرحاض", ServiceType.PLUMBING, 400, 650),
            new SeedJob("Exposed pipe burst repair", "תיקון פיצוץ בצנרת גלויה", "إصلاح انفجار أنابيب ظاهرة", ServiceType.PLUMBING, 400, 1050),
            new SeedJob("Concealed pipe burst repair", "תיקון פיצוץ בצנרת לא גלויה", "إصلاح انفجار أنابيب مخفية", ServiceType.PLUMBING, 1200, 2500),
            new SeedJob("Advanced pipe repair", "תיקון צנרת באמצעים מתקדמים", "إصلاح أنابيب بوسائل متقدمة", ServiceType.PLUMBING, 2500, 5500),
            new SeedJob("Faucet installation", "התקנת ברז", "تركيب صنبور", ServiceType.PLUMBING, 350, 650),
            new SeedJob("Sink installation", "התקנת כיור", "تركيب حوض", ServiceType.PLUMBING, 400, 700),
            new SeedJob("Toilet installation", "התקנת אסלה", "تركيب مرحاض", ServiceType.PLUMBING, 500, 800),
            new SeedJob("Angle valve installation (sink / dishwasher / washing machine)", "התקנת ברז ניל (לכיור, מדיח, מכונת כביסה)", "تركيب صنبور زاوية (للحوض، الجلاية، الغسالة)", ServiceType.PLUMBING, 300, 400),
            new SeedJob("Drain trap installation", "התקנת סיפון", "تركيب سيفون", ServiceType.PLUMBING, 320, 450),
            new SeedJob("Standard cistern installation (plastic / monobloc)", "התקנת ניאגרה רגילה (פלסטיק או מונובלוק)", "تركيب خزان مرحاض عادي (بلاستيك أو مونوبلوك)", ServiceType.PLUMBING, 450, 650),
            new SeedJob("Concealed cistern installation", "התקנת ניאגרה סמויה", "تركيب خزان مرحاض مخفي", ServiceType.PLUMBING, 2000, 2500),
            new SeedJob("Bathtub installation", "התקנת אמבטיה", "تركيب بانيو", ServiceType.PLUMBING, 4000, 5000),
            new SeedJob("Concealed mixer faucet installation", "התקנת ברז אינטרפוץ", "تركيب خلاط مدفون", ServiceType.PLUMBING, 2400, 2700),
            new SeedJob("American sink trap installation", "התקנת סיפון אמריקאי", "تركيب سيفون أمريكي", ServiceType.PLUMBING, 550, 600),
            new SeedJob("Shower base installation", "התקנת אגנית", "تركيب قاعدة دش", ServiceType.PLUMBING, 1300, 1800),
            new SeedJob("Garbage disposal installation", "התקנת טוחן אשפה", "تركيب طاحونة نفايات", ServiceType.PLUMBING, 350, 500),
            new SeedJob("Garbage disposal repair", "תיקון טוחן אשפה", "إصلاح طاحونة النفايات", ServiceType.PLUMBING, 250, 350),
            new SeedJob("Sewer unclogging", "פתיחת סתימה בביוב", "فتح انسداد الصرف الصحي", ServiceType.PLUMBING, 650, 1000),
            new SeedJob("Water pressure boost", "הגברת לחץ מים", "تعزيز ضغط المياه", ServiceType.PLUMBING, 600, 1000),
            new SeedJob("Water point installation", "התקנת נקודת מים", "تركيب نقطة مياه", ServiceType.PLUMBING, 1200, 1500),
            new SeedJob("Water manifold installation", "התקנת מרכזיית מים (חנוכיית מים)", "تركيب موزع مياه", ServiceType.PLUMBING, 1800, 2200),

            // ── ELECTRICAL (34) ──────────────────────────────────────────────
            new SeedJob("Electrician visit", "ביקור חשמלאי", "زيارة كهربائي", ServiceType.ELECTRICAL, 250, 350),
            new SeedJob("Electrical fault detection", "איתור תקלת חשמל", "كشف عطل كهربائي", ServiceType.ELECTRICAL, 300, 550),
            new SeedJob("Single-phase panel installation", "התקנת לוח חשמל חד פאזי", "تركيب لوحة كهرباء أحادية الطور", ServiceType.ELECTRICAL, 1000, 1550),
            new SeedJob("Three-phase panel installation", "התקנת לוח חשמל תלת פאזי", "تركيب لوحة كهرباء ثلاثية الطور", ServiceType.ELECTRICAL, 2500, 3150),
            new SeedJob("Main breaker replacement", "החלפת מפסק ראשי בלוח", "استبدال القاطع الرئيسي في اللوحة", ServiceType.ELECTRICAL, 350, 500),
            new SeedJob("RCD/GFCI replacement", "החלפת ממסר פחת", "استبدال قاطع التسرب الأرضي", ServiceType.ELECTRICAL, 400, 600),
            new SeedJob("Fuse replacement", "החלפת פקק בלוח", "استبدال فيوز في اللوحة", ServiceType.ELECTRICAL, 350, 450),
            new SeedJob("Panel contacts repair", "תיקון מגעים בלוח החשמל", "إصلاح ملامسات في لوحة الكهرباء", ServiceType.ELECTRICAL, 300, 450),
            new SeedJob("Shabbat timer installation", "התקנת שעון שבת", "تركيب مؤقت السبت", ServiceType.ELECTRICAL, 350, 420),
            new SeedJob("Electrical point install (no chiseling)", "התקנת נקודת חשמל - ללא חציבה", "تركيب نقطة كهرباء - بدون حفر", ServiceType.ELECTRICAL, 350, 600),
            new SeedJob("Electrical point install (with chiseling)", "התקנת נקודת חשמל - כולל חציבה", "تركيب نقطة كهرباء - مع حفر", ServiceType.ELECTRICAL, 500, 700),
            new SeedJob("Electrical point relocation", "הזזת נקודת חשמל", "نقل نقطة كهرباء", ServiceType.ELECTRICAL, 350, 450),
            new SeedJob("Power point installation", "התקנת נקודת כוח", "تركيب نقطة قوة", ServiceType.ELECTRICAL, 350, 700),
            new SeedJob("Package switch installation", "התקנת מפסק פקט", "تركيب قاطع باكيت", ServiceType.ELECTRICAL, 600, 800),
            new SeedJob("Electricity meter installation", "התקנת מונה חשמל", "تركيب عداد كهرباء", ServiceType.ELECTRICAL, 500, 700),
            new SeedJob("Outlet replacement", "החלפת שקע", "استبدال مقبس", ServiceType.ELECTRICAL, 300, 400),
            new SeedJob("Light switch replacement", "החלפת מפסק תאורה", "استبدال مفتاح إضاءة", ServiceType.ELECTRICAL, 300, 400),
            new SeedJob("Electric shutter switch replacement", "החלפת מפסק לתריס חשמלי", "استبدال مفتاح ستارة كهربائية", ServiceType.ELECTRICAL, 300, 400),
            new SeedJob("Waterproof switch/outlet replacement", "החלפת מפסק/שקע מוגן מים", "استبدال مفتاح/مقبس مقاوم للماء", ServiceType.ELECTRICAL, 350, 450),
            new SeedJob("Water heater switch replacement", "החלפת מפסק לדוד", "استبدال مفتاح سخان المياه", ServiceType.ELECTRICAL, 300, 450),
            new SeedJob("Dimmer switch installation", "התקנת מפסק דימר", "تركيب مفتاح ديمر", ServiceType.ELECTRICAL, 300, 503),
            new SeedJob("Lighting point installation", "התקנת נקודת תאורה", "تركيب نقطة إضاءة", ServiceType.ELECTRICAL, 250, 400),
            new SeedJob("Light fixture installation", "התקנת גוף תאורה", "تركيب وحدة إضاءة", ServiceType.ELECTRICAL, 250, 400),
            new SeedJob("Spotlight installation", "התקנת ספוט", "تركيب سبوت", ServiceType.ELECTRICAL, 300, 300),
            new SeedJob("Recessed light fixture installation", "התקנת גוף תאורה שקוע", "تركيب إضاءة غاطسة", ServiceType.ELECTRICAL, 250, 450),
            new SeedJob("Grounding check", "בדיקת הארקה", "فحص تأريض", ServiceType.ELECTRICAL, 400, 480),
            new SeedJob("Grounding replacement", "החלפת הארקה", "استبدال التأريض", ServiceType.ELECTRICAL, 1200, 1500),
            new SeedJob("Water heater timer installation", "התקנת טיימר לדוד", "تركيب مؤقت سخان المياه", ServiceType.ELECTRICAL, 300, 410),
            new SeedJob("Ceiling fan installation", "התקנת מאוורר תקרה", "تركيب مروحة سقف", ServiceType.ELECTRICAL, 250, 350),
            new SeedJob("Induction cooktop installation", "התקנת כיריים אינדוקציה", "تركيب موقد حث كهرومغناطيسي", ServiceType.ELECTRICAL, 450, 550),
            new SeedJob("EV charging station installation", "התקנת עמדת טעינה לרכב חשמלי", "تركيب محطة شحن سيارة كهربائية", ServiceType.ELECTRICAL, 3500, 5500),
            new SeedJob("Single-phase panel replacement", "החלפת לוח חשמל חד פאזי", "استبدال لوحة كهرباء أحادية الطور", ServiceType.ELECTRICAL, 1200, 1500),
            new SeedJob("Three-phase panel replacement", "החלפת לוח חשמל תלת פאזי", "استبدال لوحة كهرباء ثلاثية الطور", ServiceType.ELECTRICAL, 2000, 2500),
            new SeedJob("Three-phase 36A panel installation", "התקנת לוח חשמל תלת פאזי 36", "تركيب لوحة كهرباء ثلاثية الطور 36", ServiceType.ELECTRICAL, 3000, 3500)
    );
}
