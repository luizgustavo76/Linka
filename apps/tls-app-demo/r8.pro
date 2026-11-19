-keep class linka.tlsdemo.DemoActivity {
    public <init>();
    public void onCreate(android.os.Bundle);
}
-repackageclasses linka.tlsdemo.internal
# Optional JCA/JSSE and later platform APIs in BC's unused code.
-dontwarn org.bouncycastle.**

# Keep R8 shrinking and relocation while avoiding invalid verifier output.
-dontoptimize
