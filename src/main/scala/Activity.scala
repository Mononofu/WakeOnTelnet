package org.furidamu.wakeontelnet

import Constants._

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TimePicker
import android.widget.Toast
import android.widget.Button
import java.util.Calendar
import android.content.Intent
import android.app.PendingIntent
import android.app.AlarmManager
import android.content.Context
import android.app.TimePickerDialog


class MainActivity extends Activity with TypedActivity {
	var volume: Int = -40
	var muted = false
	var on = true
	var sleepTime = 10

	var NEXT_ALARM_ID = 0

	def scheduleAlarm(seconds: Int, action: Int) {
		// get a Calendar object with current time
		val cal = Calendar.getInstance()

		cal.add(Calendar.SECOND, seconds)
		val intent = new Intent(getApplicationContext(), classOf[AlarmReceiver])
		intent.putExtra("action", action)
		// In reality, you would want to have a static variable for the request 
		// code instead of 192837
		val sender = PendingIntent.getBroadcast(getApplicationContext(), NEXT_ALARM_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
		 
		// Get the AlarmManager service
		val am = getSystemService(Context.ALARM_SERVICE) match {
			  case am: AlarmManager => am
			  case _ => throw new ClassCastException
		}
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender)

		// save our alarm
		val settings = getSharedPreferences(PREFERENCES_NAME, 0)
		val editor = settings.edit()
		editor.putInt("% 2d:%02d".format(cal.get(Calendar.HOUR_OF_DAY),
									 cal.get(Calendar.MINUTE)), 
					  NEXT_ALARM_ID)
		editor.commit()

		NEXT_ALARM_ID += 1
	}

	val mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        def onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
            val c = Calendar.getInstance()
		    val curTime = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)

            val minutesToAlarm = (hourOfDay * 60 + minute - curTime + 1440) % 1440
			scheduleAlarm(minutesToAlarm*60 - c.get(Calendar.SECOND), Constants.ALARM_WAKE)
        }
    }
	
	override def onCreateDialog(id: Int) = {
		val c = Calendar.getInstance()
		id match {
	    	case Constants.TIME_DIALOG_ID => new TimePickerDialog(this, 
											    	mTimeSetListener, 
											    	c.get(Calendar.HOUR_OF_DAY), 
											    	c.get(Calendar.MINUTE), 
											    	false)
	    	case _ => null
	    }
	}

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    val status = findView(TR.textview)

    try {
    	val vol = (Commander.sendCommand("?V\r\n", true).substring(3).toInt - 161) / 2
    	findView(TR.seekview).setProgress( ((vol + 60) * 2.5).toInt )
    	volume = vol
    	findView(TR.voldisplay).text = "vol: % 2d dB".format(volume)

        Thread.sleep(100)
    	muted = Commander.sendCommand("?M\r\n", true).substring(3).toInt == 0
    	findView(TR.mutebtn).text = if(muted) "unmute" else "mute"

    	Thread.sleep(100)
    	on = Commander.sendCommand("?P\r\n", true).substring(3).toInt == 0
    	findView(TR.onbtn).text = if(on) "off" else "on"

    } catch {
    	case e: java.net.SocketTimeoutException => status.text = "timeout while getting status"
    	case _ => status.text = "other error"
    }

    findView(TR.seekview).onSeekBarChange = (progress: Int, Boolean) => {
		volume = (-60 + progress / 2.5).toInt
		findView(TR.voldisplay).text = "vol: % 2d dB".format(volume)
	}

	findView(TR.sleepbar).onSeekBarChange = (progress: Int, Boolean) => {
		sleepTime = progress
		findView(TR.sleeptime).text = "% 2d Minutes".format(progress)
	}

	findView(TR.mutebtn).onClick = () => {
		muted match {
			case false => 
				status.text = Commander.receiverMute()
				findView(TR.mutebtn).text = "unmute"

			case true => 
				status.text = Commander.receiverUnmute()
				findView(TR.mutebtn).text = "mute"
		}
		muted = !muted
	}

	findView(TR.onbtn).onClick = () => {
		on match {
			case true => 
				status.text = Commander.receiverOff()
				findView(TR.onbtn).text = "on"

			case false => 
				status.text = Commander.receiverOn()
				findView(TR.onbtn).text = "off"
		}
		on = !on
	}

	findView(TR.btn).onClick = () => status.text = Commander.setVolume(volume)

	findView(TR.tunerbtn).onClick = () => status.text = Commander.selectTuner()

	findView(TR.laptopbtn).onClick = () => status.text = Commander.selectLaptop()

	findView(TR.alarmbtn).onClick = () => showDialog(Constants.TIME_DIALOG_ID)

	findView(TR.alarmlistbtn).onClick = () => {
		val alarms = getSharedPreferences(PREFERENCES_NAME, 0).getAll()

		import scala.collection.JavaConversions._
		if(alarms.isEmpty)
			Toast.makeText(getApplicationContext(), 
					   "no alarms",
					   Toast.LENGTH_SHORT).show()
		else
			Toast.makeText(getApplicationContext(), 
					   "%s".format(alarms.keys.map(_.toString).mkString("\n")),
					   Toast.LENGTH_SHORT).show()
	}

	findView(TR.alarmcancelbtn).onClick = () => {
		val am = getSystemService(Context.ALARM_SERVICE) match {
			  case am: AlarmManager => am
			  case _ => throw new ClassCastException
		}

		val alarms = getSharedPreferences(PREFERENCES_NAME, 0).getAll()

		import scala.collection.JavaConversions._

		alarms.values.foreach{ v =>
			val id = v match {
				case id: Int => id
				case _ => throw new ClassCastException
			}
			val intent = new Intent(getApplicationContext(), classOf[AlarmReceiver])
	
			val sender = PendingIntent.getBroadcast(getApplicationContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
			am.cancel(sender)
		}

		val editor = getSharedPreferences(PREFERENCES_NAME, 0).edit()
		alarms.foreach(kv => editor.remove(kv._1))
		editor.commit()
	}

	findView(TR.sleepbtn).onClick = () => scheduleAlarm(60*sleepTime, Constants.ALARM_SLEEP)

  }
}
