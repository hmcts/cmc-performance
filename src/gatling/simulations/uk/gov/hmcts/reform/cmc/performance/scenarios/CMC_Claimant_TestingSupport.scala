
package uk.gov.hmcts.reform.cmc.performance.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment}

import scala.concurrent.duration._

object CMC_Claimant_TestingSupport {
  
  val BaseURL = Environment.baseURL
  val IdAMURL = Environment.idamURL
  val MinThinkTime = Environment.minThinkTime
  val MaxThinkTime = Environment.maxThinkTime
  val CommonHeader = Environment.commonHeader
  val paymentURL = Environment.PaymentURL
  val TotalAmount = scala.util.Random.nextInt(9999)
  
  val loginfeeder = csv ("postcodes.csv").random
  
  val home =
    
    group ("CMC_010_Homepage") {
      exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
        .exec (http ("TX01_CMC_Login_LandingLoginPage")
        .get ("/")
        .check(CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (status.is (200))
        .check (regex ("Email address")))
        .exitHereIfFailed
        .pause (MinThinkTime seconds, MaxThinkTime seconds)
    }
  
  // Enter Login credentials. This will load either postcode or dashboard
  val login = group ("CMC_020_SignIn") {
    exec (http ("TX02_CMC_Login_SubmitLogin")
      .post("${currentPage}")
      .formParam("username", "${email}")
      .formParam("password", "${password}")
      .formParam ("_csrf", "${csrf}")
      .check(regex("Claims you’ve made").optional.saveAs("existingclaimcheck"))
    )
     // .check (regex ("Find out if you can make a claim using this service")))
  }.pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val testingSupport=
    group("testingsupport") {
      exec (http ("request_88")
        .get("/testing-support")
        .check (status.in (200, 201)))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val testingSupportdraftget=
    group("testingsupportdraftget") {
      exec (http ("request_88")
        .get("/testing-support/create-claim-draft")
        .check (CsrfCheck.save)
        .check (status.in (200, 201)))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val testingSupportdraftpost=
    group("testingsupportdraftpost") {
      exec(http ("request_88")
        .post("/testing-support/create-claim-draft")
        .formParam ("_csrf", "${csrf}")
        .formParam("claim", "Create Claim Draft")
        .check (status.in (200, 201)))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val testingsupportdefemail=
    group("testingsupportdefemail") {
      exec (http ("request_88")
        .get("/claim/defendant-email")
        .check (CsrfCheck.save)
        .check (status.in (200, 201)))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val testingsupportdefemailpost=
    group("testingsupportdefemailpost") {
      exec (http ("request_88")
        .post("/claim/defendant-email")
        .formParam ("_csrf", "${csrf}")
        .formParam("address", "${defemail}")
        .check (status.in (200, 201)))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val testingsupportdefmobile=
    group("testingsupportdefmobileget") {
      exec (http ("request_88")
        .get("/claim/defendant-mobile")
        .check (CsrfCheck.save)
        .check (status.in (200, 201)))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val testingsupportdefmobilepost=
    group("testingsupportdefmobilepost") {
      exec (http ("request_88")
        .post("/claim/defendant-mobile")
        .formParam ("_csrf", "${csrf}")
        .formParam("number", "")
        .check (status.in (200, 201)))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val checkAndSend =
    group ("TX036_CMC_CheckAndSend_Checkandsend_GET") {
      exec (http ("TX036_CMC_CheckAndSend_Checkandsend_GET")
        .get ("/claim/check-and-send")
        .check (css ("input[name='_csrf']", "value").saveAs ("_csrfCardDetailPage"))
        .check (regex ("Check your answers")))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      .group ("TX08_CMC_Eligibility_ClaimType") {
        exec (http ("TX037_CMC_CheckAndSend_Checkandsend_POST")
          .post ("/claim/check-and-send")
          .formParam ("_csrf", "${_csrfCardDetailPage}")
          .formParam ("signed", "true")
          .formParam ("type", "basic")
          /*.check(regex("Claim submitted"))//this code for next 3 lines are added becuase payment is bypassed.
          .check(css(".receipt-download-container>a", "href").saveAs("pdfDownload"))
          .check(css(".reference-number>h1.bold-large").saveAs("claimNumber"))*/
          //below is the code with payments and later we need to add this code
          .check (css ("input[name='csrfToken']", "value").saveAs ("_csrfTokenCardDetailPage"))
          .check (css ("input[name='chargeId']", "value").saveAs ("CardDetailPageChargeId"))
          //.check(regex("""/card_details/(.+)',""").saveAs("_csrfCardDetailPageChargeId"))
          .check (regex ("Enter card details"))
        )
        }
    //.body(RawFileBody("RecordedSimulationCMC0412latest_0996_request.txt")))
  
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
    .group("TX038_CMC_CardDetail_SubmitCardDetail") {
      exec (http ("TX038_CMC_CardDetail_SubmitCardDetail")
        .post (paymentURL + "/card_details/${CardDetailPageChargeId}")
        .formParam ("chargeId", "${CardDetailPageChargeId}")
        .formParam ("csrfToken", "${_csrfTokenCardDetailPage}")
        .formParam ("cardNo", "4444333322221111")
        .formParam ("expiryMonth", "10")
        .formParam ("expiryYear", "22")
        .formParam ("cardholderName", "fgdfdf")
        .formParam ("cvc", "123")
        .formParam ("addressCountry", "GB")
        .formParam ("addressLine1", "4")
        .formParam ("addressLine2", "Hibernia Gardens")
        .formParam ("addressCity", "Hounslow")
        .formParam ("addressPostcode", "TW3 3SD")
        .formParam ("email", "payperftest@mailinator.com")
        .check (css ("input[name='csrfToken']", "value").saveAs ("_csrfTokenCardDetailConfirm"))
        .check (regex ("Confirm your payment")))
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
      
      // confirm the card details and submit
    .group("TX039_CMC_CardDetail_ConfirmCardDetail") {
      exec (http ("TX039_CMC_CardDetail_ConfirmCardDetail")
        .post (paymentURL + "/card_details/${CardDetailPageChargeId}/confirm")
        .formParam ("csrfToken", "${_csrfTokenCardDetailConfirm}")
        .formParam ("chargeId", "${CardDetailPageChargeId}")
        .check (regex ("Claim submitted"))
        .check (css (".receipt-download-container>a", "href").saveAs ("pdfDownload"))
        .check (css (".reference-number>h1.bold-large").saveAs ("claimNumber")))
    }
  
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
   /* .exec {
      session =>
        println("this is a pdf download url ....." + session("pdfDownload").as[String])
        println("claim number ....." + session("claimNumber").as[String])
      
        session
    }*/
      // download pdf file related to claim
    .group("TX040_CMC_PDF_Download") {
      exec (http ("TX040_CMC_PDF_Download")
        .get ("${pdfDownload}")
        .check (status.is (200)))
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
    .pause(30)
  
  val cmcLogout =
    group("TX050_CMC_Logout") {
      exec (http ("TX050_CMC_Logout")
        .get ("/logout")
        .check (regex ("Sign in")))
    }
    
}
