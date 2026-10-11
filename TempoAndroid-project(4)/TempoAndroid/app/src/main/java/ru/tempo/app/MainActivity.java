package ru.tempo.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import java.io.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
  int bg=Color.rgb(16,19,15), panel=Color.rgb(29,34,27), green=Color.rgb(183,243,107), white=Color.rgb(242,245,238), muted=Color.rgb(163,173,156);
  LinearLayout page, list; EditText buffer;
  ArrayList<String> events = new ArrayList<>();
  android.content.SharedPreferences prefs;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b); getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
    prefs=getSharedPreferences("tempo",0);
    try { String raw=prefs.getString("events",""); if(!raw.isEmpty()) events.addAll(Arrays.asList(raw.split("\\n"))); } catch(Exception ignored){}
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=0) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},10);
    render();
  }
  GradientDrawable shape(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
  TextView text(String s,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(0,3,0,3); return t; }
  LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
  LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
  void addSpace(LinearLayout l,int h){ View v=new View(this); l.addView(v,new LinearLayout.LayoutParams(1,h)); }
  TextView button(String label,Runnable run) { TextView b=text(label,15,bg,true); b.setGravity(Gravity.CENTER); b.setPadding(16,13,16,13); b.setBackground(shape(green,22)); b.setOnClickListener(v->run.run()); return b; }
  void render() {
    ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(bg);
    page=column(); page.setPadding(22,25,22,30); scroll.addView(page);
    LinearLayout header=row(); header.addView(text("tempo",31,white,true),new LinearLayout.LayoutParams(0,-2,1)); TextView chip=text("ЛИЧНЫЙ ПЛАНЕР",10,green,true); header.addView(chip); page.addView(header);
    addSpace(page,8); page.addView(text(new SimpleDateFormat("EEEE, d MMMM",new Locale("ru")).format(new Date()),14,muted,false));
    addSpace(page,24); page.addView(text("Твой день.\nБез спешки.",29,white,true));
    addSpace(page,16);
    LinearLayout quick=column(); quick.setPadding(17,17,17,17); quick.setBackground(shape(panel,20));
    quick.addView(text("ПЛАН НА СЕГОДНЯ",11,green,true)); addSpace(quick,8);
    int count=0; for(String e:events) if(e.startsWith(dayKey()+ "|")) count++;
    quick.addView(text(count+" событий сегодня",19,white,true));
    quick.addView(text("Добавляй занятия — Tempo рассчитает время выхода с учётом дороги и запаса.",13,muted,false)); page.addView(quick);
    addSpace(page,22); LinearLayout actions=row();
    actions.addView(button("+ Событие",()->addEvent()),new LinearLayout.LayoutParams(0,-2,1));
    TextView gap=text("",12,bg,false); actions.addView(gap,new LinearLayout.LayoutParams(10,1));
    actions.addView(button("Маршрут ↗",()->routeDialog()),new LinearLayout.LayoutParams(0,-2,1)); page.addView(actions);
    addSpace(page,12); page.addView(button("Импорт расписания МЭШ / CSV / ICS",()->importSchedule()),new LinearLayout.LayoutParams(-1,-2));
    addSpace(page,24); page.addView(text("РАСПИСАНИЕ",12,muted,true)); addSpace(page,8);
    list=column(); page.addView(list); showEvents();
    addSpace(page,22); page.addView(text("БЫСТРЫЕ НАСТРОЙКИ",12,muted,true)); addSpace(page,10);
    LinearLayout settings=column(); settings.setPadding(16,5,16,5); settings.setBackground(shape(panel,18));
    settingRow(settings,"Запас на дорогу","10 минут",()->numberDialog("Запас времени в минутах",10,v->{prefs.edit().putInt("buffer",v).apply(); render();}));
    settingRow(settings,"Напоминать заранее",prefs.getInt("remind",15)+" минут",()->numberDialog("Напоминать за сколько минут",15,v->{prefs.edit().putInt("remind",v).apply();render();}));
    settingRow(settings,"Город маршрутов","Москва",()->{ EditText x=new EditText(this); x.setText(prefs.getString("city","Москва")); new AlertDialog.Builder(this).setTitle("Город").setView(x).setPositiveButton("Сохранить",(d,w)->{prefs.edit().putString("city",x.getText().toString()).apply();}).setNegativeButton("Отмена",null).show(); });
    page.addView(settings);
    addSpace(page,20); page.addView(text("Tempo • данные хранятся на устройстве",11,muted,false));
    setContentView(scroll);
  }
  String dayKey(){ return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()); }
  void settingRow(LinearLayout root,String title,String value,Runnable action){ LinearLayout r=row(); r.setPadding(0,13,0,13); LinearLayout c=column(); c.addView(text(title,15,white,true)); c.addView(text(value,12,muted,false)); r.addView(c,new LinearLayout.LayoutParams(0,-2,1)); r.addView(text("›",25,green,true)); r.setOnClickListener(v->action.run()); root.addView(r); View line=new View(this); line.setBackgroundColor(Color.rgb(49,56,46)); root.addView(line,new LinearLayout.LayoutParams(-1,1)); }
  void showEvents(){
    list.removeAllViews();
    if(events.isEmpty()){ TextView empty=text("Пока пусто — добавь школу, тренировку или другое дело.",14,muted,false); empty.setPadding(15,18,15,18); empty.setBackground(shape(panel,16)); list.addView(empty); return; }
    for(int i=0;i<events.size();i++){ final int index=i; String[] p=events.get(i).split("\\|",5); if(p.length<5)continue;
      LinearLayout card=column(); card.setPadding(15,13,15,13); card.setBackground(shape(panel,16)); LinearLayout top=row();
      top.addView(text(p[1],17,white,true),new LinearLayout.LayoutParams(0,-2,1)); top.addView(text(p[0].equals(dayKey())?p[2]:p[0]+" · "+p[2],13,green,true)); card.addView(top);
      card.addView(text(p[3]+" · "+p[4],13,muted,false)); addSpace(list,8);
      card.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Удалить событие?").setMessage(p[1]).setPositiveButton("Удалить",(d,w)->{events.remove(index);save();render();}).setNegativeButton("Отмена",null).show();return true;});
      list.addView(card);
    }
  }
  void save(){ StringBuilder s=new StringBuilder(); for(String e:events){if(s.length()>0)s.append("\n");s.append(e);} prefs.edit().putString("events",s.toString()).apply(); }
  interface IntResult { void done(int value); }
  void numberDialog(String title,int def,IntResult result){ EditText e=new EditText(this); e.setInputType(2); e.setText(String.valueOf(def)); new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Сохранить",(d,w)->{try{result.done(Math.max(0,Integer.parseInt(e.getText().toString())));}catch(Exception ignored){}}).setNegativeButton("Отмена",null).show(); }
  void addEvent(){
    LinearLayout form=column(); form.setPadding(12,4,12,4);
    EditText name=new EditText(this); name.setHint("Например, Школа"); name.setTextColor(white); name.setHintTextColor(muted); form.addView(name);
    EditText date=new EditText(this); date.setHint("Дата ГГГГ-ММ-ДД"); date.setText(dayKey()); date.setTextColor(white); form.addView(date);
    EditText time=new EditText(this); time.setHint("Начало ЧЧ:ММ"); time.setText("08:30"); time.setTextColor(white); form.addView(time);
    EditText address=new EditText(this); address.setHint("Адрес / место"); address.setTextColor(white); form.addView(address);
    EditText duration=new EditText(this); duration.setHint("Дорога в минутах"); duration.setText("25"); duration.setInputType(2); duration.setTextColor(white); form.addView(duration);
    new AlertDialog.Builder(this).setTitle("Новое событие").setView(form).setPositiveButton("Сохранить",(d,w)->{
      String n=name.getText().toString().trim(), dt=date.getText().toString().trim(), tm=time.getText().toString().trim(), ad=address.getText().toString().trim(), dur=duration.getText().toString().trim();
      if(n.isEmpty()||!dt.matches("\\d{4}-\\d{2}-\\d{2}")||!tm.matches("\\d{2}:\\d{2}")){Toast.makeText(this,"Проверь название, дату и время",Toast.LENGTH_LONG).show();return;}
      events.add(dt+"|"+n+"|"+tm+"|"+(ad.isEmpty()?"Место не указано":ad)+"|дорога "+(dur.isEmpty()?"0":dur)+" мин"); save();
      scheduleReminder(n,dt,tm,ad); render();
    }).setNegativeButton("Отмена",null).show();
  }
  void scheduleReminder(String name,String date,String time,String address){
    try{
      String[] d=date.split("-"), t=time.split(":"); Calendar c=Calendar.getInstance(); c.set(Integer.parseInt(d[0]),Integer.parseInt(d[1])-1,Integer.parseInt(d[2]),Integer.parseInt(t[0]),Integer.parseInt(t[1]),0);
      int travel=0; for(String ev:events){String[] p=ev.split("\\|",5);if(p.length>=5&&p[1].equals(name)&&p[0].equals(date)){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)").matcher(p[4]);if(m.find())travel=Integer.parseInt(m.group(1));}}
      c.add(Calendar.MINUTE,-travel-prefs.getInt("buffer",10)-prefs.getInt("remind",15));
      if(c.getTimeInMillis()<=System.currentTimeMillis())return;
      Intent i=new Intent(this,ReminderReceiver.class); i.putExtra("title","Пора готовиться: "+name); i.putExtra("text","До события: "+address+". Проверь маршрут.");
      PendingIntent pi=PendingIntent.getBroadcast(this,(name+date+time).hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
      AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
      if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);}else if(Build.VERSION.SDK_INT>=23){am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);}else am.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
    }catch(Exception ignored){}
  }
  void importSchedule(){
    new AlertDialog.Builder(this).setTitle("Импорт расписания")
      .setMessage("Выбери файл расписания, если МЭШ позволяет его выгрузить. Поддерживаются CSV, TXT и календарные ICS. Прямой вход в аккаунт МЭШ пока не подключён.")
      .setPositiveButton("Выбрать файл",(d,w)->{
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/*","text/csv","text/calendar","application/octet-stream"});
        startActivityForResult(i,42);
      }).setNeutralButton("Формат CSV",(d,w)->new AlertDialog.Builder(this).setTitle("Формат CSV")
        .setMessage("Одна строка на событие, поля через запятую:\nдата,название,время,адрес,дорога_в_минутах\n\nПример:\n2026-10-12,Математика,08:30,Школа №123,20\n2026-10-12,Футбол,17:00,Спортивная улица 5,35\n\nСохрани файл в UTF-8. Для секций указывай точный адрес в поле адреса.")
        .setPositiveButton("Понятно",null).show()).setNegativeButton("Отмена",null).show();
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=42||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
    try(InputStream in=getContentResolver().openInputStream(data.getData()); ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buf=new byte[4096]; int n; while((n=in.read(buf))>0)out.write(buf,0,n);
      String content=out.toString("UTF-8"); int imported=content.contains("BEGIN:VCALENDAR")?parseIcs(content):parseCsv(content);
      save(); render(); Toast.makeText(this,"Импортировано событий: "+imported,Toast.LENGTH_LONG).show();
    }catch(Exception ex){Toast.makeText(this,"Не удалось прочитать файл. Попробуй CSV, TXT или ICS в UTF-8.",Toast.LENGTH_LONG).show();}
  }
  int parseCsv(String content){
    int added=0; String[] lines=content.replace("\r","").split("\n");
    for(String line:lines){
      String x=line.trim(); if(x.isEmpty()||x.startsWith("#")||x.toLowerCase(Locale.ROOT).startsWith("date,")||x.toLowerCase(Locale.ROOT).startsWith("дата,"))continue;
      String[] f=x.split("[,;\t]",-1); if(f.length<3)continue;
      String date=f[0].trim(), name=f[1].trim(), time=f[2].trim();
      if(!date.matches("\\d{4}-\\d{2}-\\d{2}")||!time.matches("\\d{2}:\\d{2}")||name.isEmpty())continue;
      String place=f.length>3&&!f[3].trim().isEmpty()?f[3].trim():"Место не указано";
      String travel=f.length>4?f[4].replaceAll("[^0-9]",""):"0"; if(travel.isEmpty())travel="0";
      events.add(date+"|"+name.replace("|"," ")+"|"+time+"|"+place.replace("|"," ")+"|дорога "+travel+" мин"); added++;
    }
    return added;
  }
  int parseIcs(String content){
    int added=0; String unfolded=content.replaceAll("\\r?\\n[ \t]",""); String[] blocks=unfolded.split("BEGIN:VEVENT");
    for(int i=1;i<blocks.length;i++){
      String block=blocks[i].split("END:VEVENT")[0]; String summary=icsValue(block,"SUMMARY"), start=icsValue(block,"DTSTART"), location=icsValue(block,"LOCATION");
      if(summary.isEmpty()||start.length()<8)continue;
      try{
        String date=start.substring(0,4)+"-"+start.substring(4,6)+"-"+start.substring(6,8);
        String time="09:00"; if(start.length()>=13&&start.charAt(8)=='T')time=start.substring(9,11)+":"+start.substring(11,13);
        events.add(date+"|"+summary.replace("|"," ")+"|"+time+"|"+(location.isEmpty()?"Место не указано":location.replace("|"," "))+"|дорога 0 мин"); added++;
      }catch(Exception ignored){}
    }
    return added;
  }
  String icsValue(String block,String key){
    for(String line:block.split("\\n")){ int c=line.indexOf(':'); if(c>0&&line.substring(0,c).split(";")[0].equalsIgnoreCase(key))return line.substring(c+1).replace("\\,",",").replace("\\n"," ").trim(); }
    return "";
  }
  void routeDialog(){
    LinearLayout f=column(); f.setPadding(12,4,12,4); EditText from=new EditText(this); from.setHint("Откуда"); from.setTextColor(white); f.addView(from); EditText to=new EditText(this); to.setHint("Куда"); to.setTextColor(white); f.addView(to);
    new AlertDialog.Builder(this).setTitle("Маршрут в Яндекс Картах").setView(f).setPositiveButton("Открыть",(d,w)->{
      String a=from.getText().toString().trim(), b=to.getText().toString().trim(); if(a.isEmpty()||b.isEmpty()){Toast.makeText(this,"Укажи начало и пункт назначения",Toast.LENGTH_SHORT).show();return;}
      String url="https://yandex.ru/maps/?rtext="+Uri.encode(a)+"~"+Uri.encode(b)+"&rtt=mt"; Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url)); try{startActivity(i);}catch(Exception e){Toast.makeText(this,"Не удалось открыть карты",Toast.LENGTH_SHORT).show();}
    }).setNegativeButton("Отмена",null).show();
  }
}
