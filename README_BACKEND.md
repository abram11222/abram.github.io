# دليل ربط التطبيق بـ Supabase + الأدمن + الصلاحيات

النظام الآن **Offline-first مع Supabase**: Room هو التخزين المحلي المؤقت، بينما Supabase هو مصدر البيانات المشترك بين الأجهزة.
- البيانات بقت على السيرفر (Supabase PostgreSQL) ومتعددة الأجهزة.
- Room يفضل الكاش المحلي (التطبيق يشتغل حتى من غير نت)، والكتابة تتسجل برضو على السيرفر.
- فيه تسجيل دخول (Auth) + أدوار (admin / servant / viewer) + صلاحيات على مستوى القاعدة (RLS).
- فيه لوحة أدمن جوه التطبيق لإدارة المستخدمين وتفعيل/تعطيل الحسابات وتحديد الأدوار.

---

## 1) إنشاء مشروع Supabase (مجاني)

1. ادخل على https://supabase.com واعمل **New project** (الخطة المجانية كافية).
2. افتح **SQL Editor** → **New query**.
3. افتح ملف `backend/supabase_schema.sql` اللي جوه البروجت، انسخه كله، والصقه في المحرر، واعمل **Run**.
   - ده هيبني كل الجداول + `app_users` (بالأدوار) + سياسات الصلاحيات (RLS) + storage للصور + بيانات أولية (الصفوف الدراسية والإعدادات).

## 2) إعداد المصادقة (Auth)

1. من **Authentication → Sign In / Providers**: تأكد إن **Email** مفعّل.
2. من **Authentication → Settings**: عطّل **"Confirm email"** (Enable email confirmations = OFF) علشان الحسابات الجديدة تقدر تسجل دخول طولو ما الأدمن يفعلها. (لو حابب تأكيد بالإيميل اتركه وعدّل في `AuthManager` حسب الحاجة.)

## 3) جيب مفاتيح Supabase و حطها في `.env`

1. من **Project Settings → API**، انسخ:
   - **Project URL** → ده `SUPABASE_URL`
   - **anon public** key → ده `SUPABASE_ANON_KEY`
2. جوه البروجت، اعمل ملف اسمه `.env` (جنب `.env.example`) وحط فيه:
   ```
   SUPABASE_URL=https://xxxx.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOi...anon_key...
   ```
   (أو عدّل القيم في `.env.example` مباشرةً.)
   المفاتيح دي بقت `BuildConfig` تلقائياً وقت الـcompile عن طريق Secrets Gradle Plugin.

## 4) افتح البروجت في Android Studio

1. افتح مجلد `Deacons-Service-Management-main` في Android Studio.
2. اعمل **Gradle Sync**. (لو طلب SDK/NDK، ثبّتها من SDK Manager.)
3. شغّل التطبيق (Run).

## 5) اعمل حساب الأدمن الأول

1. في التطبيق → شاشة الدخول → **إنشاء حساب جديد** بالإيميل والباسورد اللي حاببه (مثلاً إيميل الأب الكاهن المسؤول).
   - ده بيعمل auth user + صف في `app_users` بالدور الافتراضي `servant` و **غير مفعّل**.
2. ارجع لـ Supabase **SQL Editor** واعمل:
   ```sql
   update app_users set role='admin', is_active=true
       where email='YOUR_ADMIN_EMAIL@example.com';
   ```
   (بدّل الإيميل بالإيميل اللي سجلت بيه).
3. في التطبيق اعمل **تسجيل دخول** بنفس الإيميل/الباسورد → هتدخل على التطبيق وهتلاقي تبويب **"حسابي"** وفيه زرار **"إدارة المستخدمين والصلاحيات"** (بيظهر للأدمن بس).

## 6) إدارة المستخدمين والصلاحيات (جوه التطبيق)

من **حسابي → إدارة المستخدمين**:
- شوف كل الحسابات المسجلة.
- لكل حساب: اختار الدور (أدمن / خادم / مشاهدة) وفعّل/عطّل الحساب.
- زرار **مزامنة الآن** بيسحب كل بيانات السيرفر للجهاز (كمان بتحصل أوتوماتيك بعد تسجيل الدخول).

### الأدوار والصلاحيات (مطبّقة على مستوى القاعدة RLS)
| الدور | قراءة | كتابة |
|------|------|------|
| **أدمن** (admin) | كل الجداول | كل الجداول + إدارة المستخدمين |
| **خادم** (servant) | كل الجداول | المخدومين، الحضور، التقييمات (تراتيل/إنجيل/امتحانات)، الزيارات، الباركود |
| **مشاهدة** (viewer) | كل الجداول | مفيش (قراية بس) |

---

## إيه اللي اتعمل بالظبط (ملخص تقني)

- **`backend/supabase_schema.sql`**: قاعدة كاملة (15 جدول مطابين للكيانات + `app_users` + RLS + storage + seed).
- **طبقة `data/remote/`**:
  - `SupabaseConfig` / `SupabaseClient` (Retrofit + OkHttp + Moshi reflection).
  - `SupabaseApi` (PostgREST + Auth REST).
  - `AuthManager` (تسجيل دخول/خروج/تجديد جلسة Supabase + عمليات الأدمن على `app_users`).
  - `RemoteSync` (سحب كامل server→Room + mirror للكتابة Room→server).
  - `UserRole` / `AppUser`.
- **`DeaconsRepository`**: بقى offline-first — القراءة من Room زي ما هي، والكتابة بتعمل upsert/delete على Supabase كمان، + `syncFromRemote()`.
- **`DeaconsViewModel`**: بقى فيه `currentUser`/`isAdmin` + عمليات الأدمن + sync + login/signup/logout/restoreSession.
- **شاشات جديدة**: `LoginScreen`, `AdminScreen`, `AccountScreen` (جوه التطبيق).
- **`MainActivity`**: بوابة مصادقة (لازم تسجيل دخول) + تبويب "حسابي" + مسار "admin" للأدمن بس.
- **`AndroidManifest`**: أضفنا صلاحية `INTERNET` + `ACCESS_NETWORK_STATE`.
- **`.env.example`**: أضفنا `SUPABASE_URL` + `SUPABASE_ANON_KEY`.

## ملاحظات وخطوات تانية محتملة بعدين

- **تصميم أسماء الأعمدة**: أعمدة جداول البيانات في الـSQL مكتوبة camelCase بين علامات تنصيص (زي `"fullName"`, `"schoolClass"`) عشان تطابق أسماء خصائص الكيانات في الكود بالظبط، فالتطبيق بيكتب/يقرأ من غير ما نعدّل ملفات الكيانات. لو حصل إن كتابة معينة ما بتعملش mirror على السيرفر (مثلاً PostgREST ما قدرش يطابق عمود)، الحل: غيّر اسم العمود لـ snake_case في الـSQL، وضيف `@Json(name="snake_case")` على الخاصية في الكيان. (القراءة دايماً شغّالة لأن PostgREST بيرجّع الاسم زي ما هو.)
- **الصور**: حالياً `members.profileImage` بتخزن مسار/URL نصّي. لو حابب رفع فعلي للصور على Supabase Storage (bucket `member-photos` جاهز)، اربط الـupload في `PhotoManager` بـ `PUT /storage/v1/object/member-photos/{id}` وبعدها احفظ الـURL العام في `profileImage`.
- **Realtime**: المزامنة الحالية pull-based (عند الدخول + زرار مزامنة). لو حابب تحديث فوري، ممكن تفعيل Supabase Realtime subscriptions على الجداول الرئيسية وتحديث Room عند التغيير.
- **تعارض الـIDs**: عند إدخال مخدوم جديد، الجهاز بينشئ id محلي ويعمل upsert بنفس الـid على السيرفر. ده شغّال كويس طالما في جهاز رئيسي بيكتب. لو جهازين بيدخلوا مخدوم جديد في نفس الوقت من غير ما يزمزموا، ممكن يحصل تعارض على الـid — لو حصل ده، فكرة التطوير: خلّي السيرفر يولّد الـid ورجّعه (return=representation) وحدّث الـid المحلي.

## استكشاف الأخطاء
- **"حسابك غير مُفعّل"**: الأدمن معمّلش `is_active=true` للحساب — نفّل SQL اللي فوق.
- **"فشل المزامنة / HTTP 401"**: المفتاح في `.env` غلط، أو الـtoken انتهى (اعمل logout ودخول تاني).
- **HTTP 403 على كتابة**: المستخدم مش أدمن والجدول محتاج أدمن (مثلاً إدارة المجموعات/التراتيل) — ده متوقع حسب الجدول أعلاه.
- **العميل ما بيقدرش يبني**: تأكد إن `.env` فيه المفتاحين صح وإن `buildConfig = true` (موجود).

## الإصلاحات الأمنية والربط في هذه النسخة

- لا توجد كلمات مرور محفوظة داخل الهاتف ولا حساب Admin وهمي.
- تسجيل الدخول وإنشاء الحساب يتمان عبر Supabase Auth فقط.
- الجلسة تستخدم access token + refresh token، مع محاولة تجديد الجلسة عند بدء التطبيق.
- الحسابات الجديدة تُنشأ كـ servant غير مفعّل، ثم يعتمدها الأدمن من السيرفر.
- بيانات الصلاحيات (`assigned_classes` (قائمة فصول) و `can_*`) موجودة في `app_users` على Supabase ويتم فرضها أيضًا بواسطة RLS، وليس بالواجهة فقط.
- التطبيق يحتفظ بـ Room كـ offline cache، لكن لا يستخدمه كمصدر للهوية أو كلمة المرور.
- تم منع مزامنة البيانات بدون جلسة Supabase حقيقية.
- أخطاء الرفع والحذف لم تعد تُخفى؛ تظهر كفشل للمزامنة بدل اعتبار العملية ناجحة كذبًا.
- تم التخلص من النسخة المكررة داخل المشروع.
- السجلات الجديدة تستخدم IDs محلية عالية وفريدة زمنيًا لتقليل تعارض IDs بين الأجهزة عند العمل Offline.

### أول إعداد للأدمن

1. شغّل `backend/supabase_schema.sql` كاملًا في Supabase SQL Editor.
2. أنشئ حساب الأدمن من شاشة **إنشاء حساب** في التطبيق.
3. نفّذ في SQL Editor:

```sql
update public.app_users
set role = 'admin',
    is_active = true,
    can_record_attendance = true,
    can_record_assessments = true,
    can_manage_members = true,
    can_export_reports = true
where email = 'YOUR_ADMIN_EMAIL@example.com';
```

4. سجّل الدخول من التطبيق مرة أخرى.
5. من بعدها أي خادم ينشئ حسابًا من شاشة إنشاء الحساب سيظهر كـ **طلب قيد المراجعة** في Admin Dashboard، ويمكن للأدمن تفعيله وتحديد الفصل والصلاحيات.

> لا تضع `service_role` key داخل تطبيق Android. إنشاء Auth users إداريًا عبر service_role يحتاج Backend/Edge Function منفصل، وليس مفتاحًا سريًا داخل الـAPK.


## نطاق الفصول
- الأدمن يرى كل الفصول.
- الخادم العادي يحصل على قائمة `assigned_classes` ويمكن للأدمن اختيار أكثر من فصل لكل خادم.
- RLS تمنع الخادم من قراءة أو تعديل مخدومين/حضور/تقييمات خارج الفصول المسموح بها.
- `assigned_class` القديم محفوظ للتوافق، ويُملأ بأول فصل فقط؛ المصدر الأساسي الجديد هو `assigned_classes`.


## Roles and approval flow

- `super_admin`: owner-level administrator. Can manage other admins, servants, viewers, permissions, and all application data.
- `admin`: full application/data administration, but cannot create, delete, disable, or change another admin.
- `servant`: access is limited by the classes and granular permissions assigned by an admin.
- `viewer`: read-only access where allowed.

New accounts are created through Supabase Auth from the app and start inactive (`Pending`). An active admin approves the account and assigns role, classes, and permissions. Database RLS enforces these rules server-side.

After applying a schema update, re-run the full `backend/supabase_schema.sql` in Supabase SQL Editor.


## Final cloud architecture

- Supabase Auth is the only authentication source. Passwords are never stored in Room or SharedPreferences.
- New registrations create an inactive `app_users` row through the Auth trigger and show **Pending** until an Admin approves them.
- Roles: `super_admin`, `admin`, `servant`, `viewer`. Super Admins can manage Admins; regular Admins manage servants/viewers.
- Servants can be assigned multiple class names and permissions. RLS restricts both reads and writes to their assigned classes.
- Room remains the offline cache. Local writes that cannot reach Supabase are stored in a persistent pending-operation queue and retried on the next sync.
- Normal sync never pushes the entire Room database back to Supabase. It flushes pending writes, then replaces the local cache with the authenticated user's current RLS-filtered server snapshot.
- Member photos are uploaded to the `member-photos` Storage bucket when a local photo is synchronized; the remote row stores a stable Storage URL.
- Account removal from the Android client is implemented as deactivation so an Auth account is never orphaned. Permanent Auth deletion must be a server-side admin operation.
- The Android app contains only the Supabase anon key. Never add a `service_role` key to the APK.

### Setup order

1. Run `backend/supabase_schema.sql` in Supabase SQL Editor.
2. Create the first Auth user with `abramonetwo@gmail.com`.
3. Run the same SQL file again if the Auth user was created after the first schema run; the final bootstrap statement promotes that account to `super_admin`.
4. Build the Android app.
5. Test: Admin login → new servant signup → Pending → Admin approval → class/permission assignment → servant login → attendance/member change → sync back to Admin.
