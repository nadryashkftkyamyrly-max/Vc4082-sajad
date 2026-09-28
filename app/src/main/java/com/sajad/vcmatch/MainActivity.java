package com.sajad.vcmatch;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
    EditText frequency,inA,inB,ns,targetR,vPrimary,ae,bmax,al,lCoil,rCoil,rSoil;
    Spinner inputMode;
    TextView result;
    SharedPreferences sp;
    final String[] modes={"Rs / Xs از VC4082","|Z| / θ از VC4082"};

    @Override public void onCreate(Bundle b){super.onCreate(b); setContentView(R.layout.activity_main);
        frequency=f(R.id.frequency); inA=f(R.id.inA); inB=f(R.id.inB); ns=f(R.id.ns); targetR=f(R.id.targetR);
        vPrimary=f(R.id.vPrimary); ae=f(R.id.ae); bmax=f(R.id.bmax); al=f(R.id.al); lCoil=f(R.id.lCoil); rCoil=f(R.id.rCoil); rSoil=f(R.id.rSoil);
        inputMode=findViewById(R.id.inputMode); result=findViewById(R.id.result);
        inputMode.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        inputMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id){
                if(pos==0){inA.setHint("Rs (Ω)"); inB.setHint("Xs (Ω؛ با علامت)");}
                else {inA.setHint("|Z| (Ω)"); inB.setHint("θ (درجه؛ با علامت)");}
            } public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        sp=getSharedPreferences("vc4082",MODE_PRIVATE); load();
        findViewById(R.id.calc).setOnClickListener(v->calculate());
        findViewById(R.id.save).setOnClickListener(v->{save(); Toast.makeText(this,"ورودی‌ها ذخیره شد",Toast.LENGTH_SHORT).show();});
        findViewById(R.id.clear).setOnClickListener(v->clearAll());
    }
    @SuppressWarnings("unchecked") private <T extends View> T f(int id){return (T)findViewById(id);}
    private double d(EditText e) throws Exception { String s=e.getText().toString().trim().replace(',','.'); if(s.isEmpty()) throw new Exception("یکی از ورودی‌های ضروری خالی است"); return Double.parseDouble(s); }
    private Double opt(EditText e){try{String s=e.getText().toString().trim().replace(',','.'); return s.isEmpty()?null:Double.parseDouble(s);}catch(Exception x){return null;}}
    private String n(double v,String unit){ double a=Math.abs(v); if(a>=1e6)return String.format(Locale.US,"%.3f M%s",v/1e6,unit); if(a>=1e3)return String.format(Locale.US,"%.3f k%s",v/1e3,unit); if(a<1e-6&&a>0)return String.format(Locale.US,"%.3f n%s",v*1e9,unit); if(a<1e-3&&a>0)return String.format(Locale.US,"%.3f µ%s",v*1e6,unit); return String.format(Locale.US,"%.3f %s",v,unit); }
    private String reactiveMatch(double x,double f){
        if(Math.abs(x)<0.01) return "نیاز به جبران راکتیو محسوس نیست (X≈0).";
        if(x>0){double c=1.0/(2*Math.PI*f*x); return "خازن سری ≈ "+cap(c)+"  (برای جبران +jX)";}
        double l=Math.abs(x)/(2*Math.PI*f); return "سلف سری ≈ "+ind(l)+"  (برای جبران −jX)";
    }
    private String cap(double c){if(c>=1e-6)return String.format(Locale.US,"%.3f µF",c*1e6); if(c>=1e-9)return String.format(Locale.US,"%.3f nF",c*1e9); return String.format(Locale.US,"%.3f pF",c*1e12);}
    private String ind(double l){if(l>=1)return String.format(Locale.US,"%.3f H",l); if(l>=1e-3)return String.format(Locale.US,"%.3f mH",l*1e3); return String.format(Locale.US,"%.3f µH",l*1e6);}
    private void calculate(){
      try{
        double f=d(frequency), a=d(inA), b=d(inB), Ns=d(ns), Rt=d(targetR), Vp=d(vPrimary), Ae=d(ae)*1e-6, B=d(bmax);
        if(f<=0||Ns<=0||Rt<=0||Vp<0||Ae<=0||B<=0) throw new Exception("ورودی‌ها باید مثبت و معتبر باشند");
        double Rs,Xs,Z,theta;
        if(inputMode.getSelectedItemPosition()==0){Rs=a; Xs=b; Z=Math.hypot(Rs,Xs); theta=Math.toDegrees(Math.atan2(Xs,Rs));}
        else {Z=a; theta=b; double r=Math.toRadians(theta); Rs=Z*Math.cos(r); Xs=Z*Math.sin(r);}
        if(Rs<=0) throw new Exception("Rs باید بزرگ‌تر از صفر باشد");

        double ratio=Math.sqrt(Rs/Rt);
        double NpMatch=Ns/ratio;
        double Nmin=Vp/(4.44*f*Ae*B);
        double Np=Math.ceil(Math.max(NpMatch,Nmin));
        double actualRatio=Ns/Np;
        double Rpri=Rs/(actualRatio*actualRatio), Xpri=Xs/(actualRatio*actualRatio), Zpri=Math.hypot(Rpri,Xpri);
        double thPri=Math.toDegrees(Math.atan2(Xpri,Rpri));
        double Vs=Vp*actualRatio;
        double flux=Vp/(4.44*f*Np*Ae);
        double IsecMatched=Vs/Rs;
        double Pmatched=IsecMatched*IsecMatched*Rs;
        double IpriApprox=Vp>0?Pmatched/Vp:0;

        StringBuilder s=new StringBuilder();
        s.append("ورودی معادل بار در ").append(String.format(Locale.US,"%.1f Hz",f)).append("\n");
        s.append("Rs = ").append(n(Rs,"Ω")).append("\nXs = ").append(n(Xs,"Ω")).append("\n|Z| = ").append(n(Z,"Ω")).append("\nθ = ").append(String.format(Locale.US,"%.2f°",theta)).append("\n\n");
        s.append("مچینگ بعد از ترانس\n");
        s.append("• ").append(reactiveMatch(Xs,f)).append("\n");
        s.append("• بعد از جبران ایده‌آل: Z ≈ ").append(n(Rs,"Ω")).append(" مقاومتی\n\n");
        s.append("محاسبه نسبت دور برای بار هدف آمپ\n");
        s.append("• نسبت ایده‌آل Ns/Np ≈ ").append(String.format(Locale.US,"%.3f",ratio)).append("\n");
        s.append("• Np از نظر تطبیق مقاومت ≈ ").append(String.format(Locale.US,"%.1f دور",NpMatch)).append("\n");
        s.append("• حداقل Np از نظر شار هسته ≈ ").append(String.format(Locale.US,"%.1f دور",Nmin)).append("\n");
        s.append("• Np پیشنهادی محاسباتی = ").append(String.format(Locale.US,"%.0f دور",Np)).append("\n");
        if(NpMatch<Nmin) s.append("⚠ تطبیق هدف با این Ns و ولتاژ، دوری کمتر از حد شار می‌خواهد؛ باید Ns/بار هدف/ولتاژ تغییر کند.\n");
        s.append("• نسبت واقعی با Np پیشنهادی = 1:").append(String.format(Locale.US,"%.3f",actualRatio)).append("\n");
        s.append("• Vs بی‌باری ایده‌آل ≈ ").append(String.format(Locale.US,"%.1f Vrms",Vs)).append("\n");
        s.append("• Bpeak تخمینی ≈ ").append(String.format(Locale.US,"%.3f T",flux)).append("\n\n");
        s.append("بار بازتابی روی اولیه (قبل از مچ اولیه)\n");
        s.append("• Zpri ≈ ").append(String.format(Locale.US,"%.3f + j%.3f Ω",Rpri,Xpri)).append("\n");
        s.append("• |Zpri| ≈ ").append(String.format(Locale.US,"%.3f Ω",Zpri)).append(" ، θ ≈ ").append(String.format(Locale.US,"%.2f°",thPri)).append("\n\n");
        s.append("مچینگ قبل از ترانس\n");
        s.append("• ").append(reactiveMatch(Xpri,f)).append("\n");
        s.append("• بعد از جبران ایده‌آل، آمپ تقریباً ").append(String.format(Locale.US,"%.2f Ω",Rpri)).append(" مقاومتی می‌بیند.\n\n");

        Double AL=opt(al);
        if(AL!=null && AL>0){double Lm=AL*1e-9*Np*Np; double Xm=2*Math.PI*f*Lm; s.append("هسته / AL\n• Lm اولیه ≈ ").append(ind(Lm)).append("\n• Xm در فرکانس کاری ≈ ").append(n(Xm,"Ω")).append("\n\n");}
        s.append("برآورد ایده‌آل در حالت مچ‌شده و بدون محدودیت توان منبع\n");
        s.append("• Isec ≈ ").append(String.format(Locale.US,"%.3f Arms",IsecMatched)).append("\n");
        s.append("• Pactive بار ≈ ").append(String.format(Locale.US,"%.1f W",Pmatched)).append("\n");
        s.append("• جریان اولیه ایده‌آل تقریبی ≈ ").append(String.format(Locale.US,"%.2f A",IpriApprox)).append("\n");
        s.append("⚠ این بخش فقط وقتی معتبر است که آمپ/بوست واقعاً این توان و جریان را تأمین کنند.\n");

        Double Lc=opt(lCoil), Rc=opt(rCoil), Rsoil=opt(rSoil);
        if(Lc!=null && Lc>0){double xl=2*Math.PI*f*Lc/1000.0; double ql=IsecMatched*IsecMatched*xl; double vl=IsecMatched*xl; s.append("\nکویل (اختیاری)\n• XL = ").append(n(xl,"Ω")).append("\n• QL ≈ ").append(String.format(Locale.US,"%.1f VAR",ql)).append("\n• VL ≈ ").append(String.format(Locale.US,"%.1f Vrms",vl)).append("\n"); if(Rc!=null&&Rc>=0)s.append("• تلفات مسی کویل ≈ ").append(String.format(Locale.US,"%.1f W",IsecMatched*IsecMatched*Rc)).append("\n");}
        if(Rsoil!=null && Rsoil>=0){s.append("• ولتاژ مقاومتی بین الکترودها ≈ ").append(String.format(Locale.US,"%.1f Vrms",IsecMatched*Rsoil)).append("\n"); s.append("• توان اکتیو تقریبی خاک ≈ ").append(String.format(Locale.US,"%.1f W",IsecMatched*IsecMatched*Rsoil)).append("\n");}

        result.setText(s.toString());
      }catch(Exception e){result.setText("خطا: "+e.getMessage());}
    }
    private void save(){SharedPreferences.Editor e=sp.edit(); int[] ids={R.id.frequency,R.id.inA,R.id.inB,R.id.ns,R.id.targetR,R.id.vPrimary,R.id.ae,R.id.bmax,R.id.al,R.id.lCoil,R.id.rCoil,R.id.rSoil}; for(int id:ids){EditText x=findViewById(id); e.putString("e"+id,x.getText().toString());} e.putInt("mode",inputMode.getSelectedItemPosition()).apply();}
    private void load(){int[] ids={R.id.frequency,R.id.inA,R.id.inB,R.id.ns,R.id.targetR,R.id.vPrimary,R.id.ae,R.id.bmax,R.id.al,R.id.lCoil,R.id.rCoil,R.id.rSoil}; for(int id:ids){String x=sp.getString("e"+id,null); if(x!=null)((EditText)findViewById(id)).setText(x);} inputMode.setSelection(sp.getInt("mode",0));}
    private void clearAll(){inA.setText("");inB.setText("");lCoil.setText("");rCoil.setText("");rSoil.setText("");result.setText("");}
}
