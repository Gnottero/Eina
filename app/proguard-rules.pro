# R8 rules for the release build.
# Room, Compose and Koin (constructor DSL, not reflective) work with the consumer rules shipped by
# the libraries; only the cases R8 cannot infer are kept here.

# Room entities are instantiated by generated code, which is itself obfuscatable: the fields are
# kept so the column mappings survive.
-keepclassmembers class com.eina.app.data.db.** { <fields>; }

# TypeConverters serialise enums by name: renaming them would change values already written to the
# database.
-keepclassmembers enum com.eina.app.data.db.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room/SQLite exceptions are inspected by name in crash logs.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# org.json ships with the Android framework: no rule needed for the seeder.
