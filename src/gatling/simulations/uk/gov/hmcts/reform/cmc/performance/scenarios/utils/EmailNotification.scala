package uk.gov.hmcts.reform.cmc.performance.scenarios.utils

import java.io.{BufferedWriter, FileWriter}

import io.gatling.core.Predef._
import uk.gov.service.notify.{NotificationClient, NotificationList}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.util.matching.Regex

object EmailNotification {
  
  val getPin = {
    exec {
      session =>
        val client = new NotificationClient ("genericteam-37ba0bfe-68ed-4e8e-9e19-f3a4cc331d80-f3254537-8758-457d-8c8f-41cc2833bea4")
        val str = findEmailCMC(client, session("defemail").as[String])
        val patterncode = new Regex ("code: [a-zA-Z0-9]{8}")
        val patternclaim = new Regex ("number: [a-zA-Z0-9]{8}")
        session.setAll("securitycode" -> patterncode.findFirstMatchIn(str.get).mkString.trim.replace("code: ",""),
          "claimref" -> patternclaim.findFirstMatchIn(str.get).mkString.trim.replace("number: ","")
        )
    }
  }
      .pause(30)
  .exec {
      session =>
        println("securitycode " + session("securitycode").as[String])
        println("claimref " + session("claimref").as[String])
        //println("security pin " + session("securitypin").as[String])
        session
    }
    .doIf("${securitycode.exists()}") {
    exec {
      session =>
        val fw = new BufferedWriter (new FileWriter ("defendantdetails.csv", true))
        try {
          fw.write (session ("claimref").as [ String ] + "," + session ("securitycode").as [ String ] + "," + session ("defemail").as [ String ]+ "," + session ("email").as [ String ] + "\r\n")
        }
        finally fw.close ()
        session
    }
  }
  
  
  
  def findEmail(client: NotificationClient, emailAddress:String) : Option[String] = {
    var emailBody = findEmailByStatus(client, emailAddress, "created")
    if (emailBody.isDefined) {
      return emailBody
    }
    emailBody = findEmailByStatus(client, emailAddress, "sending")
    if (emailBody.isDefined) {
      return emailBody
    }
    emailBody = findEmailByStatus(client, emailAddress, "delivered")
    if (emailBody.isDefined) {
      return emailBody
    }
    findEmailByStatus(client, emailAddress, "failed")
  }
  
  def findEmailByStatus(client: NotificationClient, emailAddress: String, status: String) : Option[String] = {
    val notificationList = client.getNotifications(status, "email", null, null)
    println("Searching notifications from " + status)
    val emailBody = getEmailBodyByEmailAddress(notificationList, emailAddress)
    if (emailBody.isDefined) {
      return emailBody
    }
    None
  }
  
  def getEmailBodyByEmailAddress(notifications: NotificationList, emailAddress: String) : Option[String] = {
    for(notification <- notifications.getNotifications.asScala) {
      if (notification.getEmailAddress.get().equalsIgnoreCase(emailAddress)) {
        println("Found match for email " + emailAddress)
        return Some(notification.getBody)
      } else {
        println("Comparing " + notification.getEmailAddress.get() + " with " + emailAddress)
      }
    }
    None
  }
  
  def findEmailCMC(client: NotificationClient, emailAddress: String): Option[String] = {
    val emailBody = findEmailFromNotify(client, emailAddress)
    if (emailBody.isDefined) {
      return emailBody
    }
    None
  }
  
  def findEmailFromNotify(client: NotificationClient, emailAddress: String): Option[String] = {
    var notificationList = client.getNotifications(null, "email", null, null)
    println(s"Searching notifications for $emailAddress")
    var emailBody = getEmailBodyFromNotifyEmail(notificationList, emailAddress)
    var i = 1
    val maxNotifyPages = 3
    while (i < maxNotifyPages && emailBody.isEmpty && notificationList.getNextPageLink.isPresent) {
      val nextPageLink = notificationList.getNextPageLink.get()
      println(s"Searching notify emails in next $i page $nextPageLink")
      val pattern = raw"older_than=([0-9a-zA-Z\-]*)&?".r
      val olderThanId = pattern.findFirstMatchIn(nextPageLink).get.group(1)
      notificationList = client.getNotifications(null, "email", null, olderThanId)
      emailBody = getEmailBodyFromNotifyEmail(notificationList, emailAddress)
      i += 1
    }
    if (emailBody.isDefined) {
      return emailBody
    }
    None
  }
  
  def getEmailBodyFromNotifyEmail(notifications: NotificationList, emailAddress: String): Option[String] = {
    for (notification <- notifications.getNotifications.asScala) {
      if (notification.getEmailAddress.get().equals(emailAddress)) {
        println(s"Found match for email $emailAddress")
        return Some(notification.getBody)
      }
    }
    None
  }

}
