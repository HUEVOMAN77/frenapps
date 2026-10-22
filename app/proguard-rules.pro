# Reglas de R8 para el build de release (minifyEnabled true).
# Arriba no habia ninguna: minify estaba activo con solo las reglas por defecto.

# Room resuelve la implementacion generada ("<Db>_Impl") por nombre en runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class *

# El binder de Shizuku y su proxy se invocan por IPC, nunca desde codigo propio.
-keep class rikka.shizuku.** { *; }
-keep class android.content.IContentProvider { *; }

# WorkManager instancia el worker por nombre desde el sistema.
-keep class com.northmendo.Appzuku.AutoKillWorker { *; }

# Tiles del panel rapido: el sistema los levanta sin referencia estatica.
-keep class com.northmendo.Appzuku.ShappkyQuickTile { *; }
-keep class com.northmendo.Appzuku.ShappkyBackgroundKillTile { *; }

# Generics y anotaciones que lectura/reflexion de Room y del JSON de backup.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
