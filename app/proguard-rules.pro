# Regole R8 per la build release.
# Room, Compose e Koin (DSL a costruttori, non riflessivo) funzionano con le regole
# consumer delle librerie: qui restano solo i casi che R8 non puo' dedurre.

# Le entity Room vengono istanziate dal codice generato, che pero' e' esso stesso
# offuscabile: si tengono i campi per non rompere i mapping di colonna.
-keepclassmembers class com.eina.app.data.db.** { <fields>; }

# I TypeConverter serializzano gli enum per nome: rinominarli cambierebbe i valori
# gia' scritti nel database.
-keepclassmembers enum com.eina.app.data.db.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Le eccezioni di Room/SQLite vengono ispezionate per nome nei log di crash.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# org.json e' nel framework Android: nessuna regola necessaria per il seeder.
