package org.furidamu.wakeontelnet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import java.util.Calendar
 
class AlarmReceiver extends BroadcastReceiver {

 override def onReceive(context: Context, intent: Intent) {
   try {
     val message = intent.getIntExtra("action", -1)

     message match {
         case Constants.ALARM_WAKE =>
            Commander.receiverOn()
            Thread.sleep(10)
            Commander.selectTuner()
            Thread.sleep(10)
            Commander.setVolume(-42)
         case Constants.ALARM_SLEEP =>
            Commander.receiverOff()
     }

    val c = Calendar.getInstance()
    val editor = context.getSharedPreferences(Constants.PREFERENCES_NAME, 0).edit()
    editor.remove("% 2d:%02d".format(c.get(Calendar.HOUR_OF_DAY),
                             c.get(Calendar.MINUTE)))
    editor.commit()


    } catch {
    	case e: Exception => 
     		Toast.makeText(context, "There was an error somewhere, but we still received an alarm", Toast.LENGTH_SHORT).show()
     		e.printStackTrace()
 
    }
 }
 
}