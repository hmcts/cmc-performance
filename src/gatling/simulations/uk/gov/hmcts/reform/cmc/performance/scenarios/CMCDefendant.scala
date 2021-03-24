package uk.gov.hmcts.reform.cmc.performance.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CsrfCheck.{csrfParameter, csrfTemplate}
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CurrentPageCheck.currentPageTemplate
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment}

import scala.concurrent.duration._

object CMCDefendant {
  
  val BaseURL = Environment.baseURL
  val baseDomain=Environment.baseDomain
  val IdAMURL = Environment.idamURL
  val MinThinkTime = Environment.minThinkTime
  val MaxThinkTime = Environment.maxThinkTime
  val CommonHeader = Environment.commonHeader
  val paymentURL = Environment.PaymentURL
  val TotalAmount = scala.util.Random.nextInt(9999)
  
  
  val landingPage =
    group("CMCDef_010_LandingPage_Get") {
      exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
        .exec (http ("LandingPage_Get")
        .get ("/first-contact/start")
          .headers(Environment.headers_firstcontact)
        .check (CsrfCheck.save)
        .check (regex ("Start now")))
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val startPage=
    group("CMCDef_020_LandingPage_Post") {
      exec (http ("LandingPage_Post")
        .post ("/first-contact/start")
          .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("start-button", "Start now")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Enter your claim number"))
      )
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val claimNumber =
    group("CMCDef_030_ClaimNumber") {
      exec (http ("ClaimNumber")
        .post (currentPageTemplate)
        .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("reference", "${claimno}")
        .check(css("input[name='redirect_uri']", "value").saveAs("redirectUri"))
        .check(css("input[name='client_id']", "value").saveAs("clientId"))
        .check(css("input[name='state']", "value").saveAs("state"))
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        .check (regex ("Enter security code"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val enterpinPost=
    group("CMCDef_040_PinPost"){
      exec (http ("Def_PinPost")
        .post (IdAMURL+"/loginWithPin")
       // .post (currentPageTemplate)
        .headers(Environment.headers_withpin)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("pin","${pin}")
        .formParam ("redirect_uri","${redirectUri}")
        .formParam ("client_id","${clientId}")
        .formParam ("state","${state}")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Claim details"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  
  val ClaimSummary =
    group("CMCDef_050_ClaimSummary") {
      exec (http ("ClaimSummary")
        .post (currentPageTemplate)
          .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .check (CurrentPageCheck.save)
       // .check (CsrfCheck.save)
        .check (regex ("Create an account or sign in"))
      )
    }
      
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val loginAsDefendantGet =
    group("CMCDef_060_Login_As_DefendantGet") {
      exec (http ("Login_As_DefendantGet")
        .get (currentPageTemplate)
          .check(status.in(201,200,204))
        .check(regex("response_type=code&state=(.*)&client_id").saveAs("state"))
        .check(regex("&redirect_uri=(.*)&jwt=").saveAs("redirectURI"))
        .check(regex("&client_id=(.*)&redirect_uri").saveAs("clientId"))
        .check(regex("&scope=&jwt=(.*)\">Sign in to your account").saveAs("jwttoken"))
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val loginAsDefendant=
    group("CMCDef_070_Login_As_Defendant") {
        exec (http ("Login_As_Defendant")
        .post(IdAMURL+"/register?redirect_uri=${redirectURI}&client_id=${clientId}&state=${state}&scope=&jwt=${jwttoken}")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("username", "${defuser}")
        .formParam ("password", "Pa55word11")
        .check(status.in(200,201,204))
        .check (regex (" Claims made against you"))
      )
        //.check (CurrentPageCheck.save)
        //.check (CsrfCheck.save))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  /*========================================================================================
  // below are the defendant response details
  =========================================================================================*/
  
  val dashboard =
    group ("CMCDefRes_010_Dashboard") {
      exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
        .exec (http ("Dashboard")
          .get ("/dashboard")
          .check(CurrentPageCheck.save)
          .check (CsrfCheck.save)
          .check (status.is (200))
          .check (regex ("Email address")))
        .exitHereIfFailed
        .pause (MinThinkTime seconds, MaxThinkTime seconds)
    }
  
  // Enter Login credentials. This will load either postcode or dashboard
  val defendantlogin =
    group ("CMCDefRes_020_SignIn") {
    exec (http ("Signin")
      .post ("${currentPage}")
      .formParam("username", "${defemail}")
      .formParam("password", "Pa55word11")
      .formParam("save", "Sign in")
      .formParam("selfRegistrationEnabled", "true")
      .formParam ("_csrf", "${csrf}")
      .check(regex("Claims made against you"))
      /*.check(substring("""<a href="/dashboard/""").count.saveAs("claimCount")) // count for how many draft cases*/
      .check(regex("""<a href="/dashboard/(.+)/defendant"""").find(1).optional.saveAs("claimId"))
    )
    // .check (regex ("Find out if you can make a claim using this service")))
  }.pause (MinThinkTime seconds, MaxThinkTime seconds)
  
    /*.exec {
    session =>
      val fw = new BufferedWriter (new FileWriter ("defendantclaimsnumber.csv", true))
      try {
        fw.write (session ("claimCount").as [ String ] + "," + session ("claimId").as [ String ] + "," + session ("defemail").as [ String ]+ "," + session ("defemail").as [ String ] + "\r\n")
      }
      finally fw.close ()
      session
  }*/
  
  val casetaskList =
    group("CMCDefRes_030_Tasklist") {
      exec (http ("TaskList")
        .get ("/case/${claimId}/response/task-list")
        //.check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
      
        .group("CMCDefRes_030_YourDetailsGet") {
        exec (http ("YourDetails_Get")
          .get ("/case/${claimId}/response/your-details")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
        )
      }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val defendantDetails=
    group("CMCDefRes_040_YourDetailsPost") {
      exec (http ("Response_YourDetails_Post")
        .post (currentPageTemplate)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("type", "individual")
        .formParam ("address[line1]", "Flat 3A")
        .formParam ("address[line2]", "Street 1")
        .formParam ("address[line3]", "Middle Road")
        .formParam ("address[city]", "London")
        .formParam ("address[postcode]", "SW1H 9AJ")
        .formParam ("hasCorrespondenceAddress", "false")
        .formParam ("correspondenceAddress[postcodeLookup]", "")
        .formParam ("correspondenceAddress[addressList]", "")
        .formParam ("correspondenceAddress[line1]", "")
        .formParam ("correspondenceAddress[line2]", "")
        .formParam ("correspondenceAddress[line3]", "")
        .formParam ("correspondenceAddress[city]", "")
        .formParam ("correspondenceAddress[postcode]", "")
        .formParam ("correspondenceAddress[addressVisible]", "true")
        .formParam ("correspondenceAddress[addressSelectorVisible]", "false")
        .formParam ("correspondenceAddress[enterManually]", "false")
        .formParam ("saveAndContinue", "Save and continue")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        .check (regex ("Enter your date of birth"))
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val dob=
    group("CMCDefRes_050_DOB") {
      exec (http ("Response_DOB")
        .post (currentPageTemplate)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("known", "true")
        .formParam ("date[day]", "01")
        .formParam ("date[month]", "08")
        .formParam ("date[year]", "1978")
        .formParam ("saveAndContinue", "Save and continue")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        // .check(regex("Add a contact number"))
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val mobile=
    group("CMCDefRes_060_Mobile") {
      exec (http ("Response_Mobile")
        .post (currentPageTemplate)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("number", "07548723412")
        .formParam ("saveAndContinue", "Save and continue")
        //.check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  //need to add this here-https://moneyclaim.nonprod.platform.hmcts.net/case/6e99f589-5a25-4abf-976a-49622380d6bf/response/task-list
  
  val moreTimeRequest=
    group("CMCDefRes_070_MoreTimeRequestGet") {
      exec (http ("Response_MoreTimeRequest_Get")
        // .get("/case/38ea3269-a024-46f3-a6ca-03828f5f3da6/response/more-time-request"))
        .get (currentPageTemplate)
        .check (CsrfCheck.save)
      )
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
    .group("CMCDefRes_070_MoreTimeRequestGet") {
        exec (http ("Response_MoreTimeRequest_Post")
          .post ("/case/${claimId}/response/more-time-request")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("option", "no")
          .formParam ("saveAndContinue", "Save and continue")
          //.check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          // .check(regex("Do you want more time to respond?"))
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  val responseType=
    group("CMCDefRes_080_ResponseType") {
      exec (http ("Response_ResponseType_Get")
        .get("/case/${claimId}/response/response-type")
        .check (CsrfCheck.save))
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
      
        .group("Response_ResponseType_Post")
        {
        exec (http ("Response_ResponseType_Post")
          .post ("/case/${claimId}/response/response-type")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("type[value]", "DEFENCE")
          .formParam ("saveAndContinue", "Save and continue")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //.check(regex("How do you respond to the claim?"))
    
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val reject_All_Claims=
    group("CMCDefRes_090_RejectAllClaim") {
     /* exec (http ("Response_Reject_All_Of_Claim_Get")
        // .get("/case/38ea3269-a024-46f3-a6ca-03828f5f3da6/response/reject-all-of-claim")
        .get (currentPageTemplate))*/
        exec (http ("Response_Reject_All_Of_Claim_Post")
          .post (currentPageTemplate)
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("option", "dispute")
          .formParam ("saveAndContinue", "Save and continue")
          //.check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Why do you reject the claim?"))
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  val yourDefence=
    group("CMCDefRes_100_YourDefence") {
      exec (http ("Response_Your_Defence_Post_Get")
        .get ("/case/${claimId}/response/your-defence")
        .check (CsrfCheck.save))
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
       // .get (currentPageTemplate))
  group("CMCDefRes_100_YourDefencePost"){
        exec (http ("Response_Your_Defence_Post")
          .post ("/case/${claimId}/response/your-defence")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("text", "adadasdfadad")
          .formParam ("saveAndContinue", "Save and continue")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //.check(regex("Why do you disagree with the claim?"))
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val timeLine=
    group("CMCDefRes_110_Timeline") {
      exec (http ("Response_TimeLine")
        .post ("/case/${claimId}/response/timeline")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("rows[0][date]", "1 Sep 2020")
        .formParam ("rows[0][description]", "asasasasasasas")
        .formParam ("rows[1][date]", "3 Jan 2021")
        .formParam ("rows[1][description]", "asasasas")
        .formParam ("rows[2][date]", "")
        .formParam ("rows[2][description]", "")
        .formParam ("rows[3][date]", "")
        .formParam ("rows[3][description]", "")
        .formParam ("comment", "")
        .formParam ("saveAndContinue", "Save and continue")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        	.check(regex("Add your timeline of events"))
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val evidence=
    group("CMCDefRes_120_ResponseEvidence") {
      exec (http ("Response_Evidence")
        .post ("/case/${claimId}/response/evidence")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("rows[0][type]", "RECEIPTS")
        .formParam ("rows[0][description]", "asasas")
        .formParam ("rows[1][type]", "PHOTO")
        .formParam ("rows[1][description]", "asasasasa")
        .formParam ("rows[2][type]", "")
        .formParam ("rows[2][description]", "")
        .formParam ("rows[3][type]", "")
        .formParam ("rows[3][description]", "")
        .formParam ("comment", "")
        .formParam ("saveAndContinue", "Save and continue")
       // .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  val freeMediation =
    group("CMCDefRes_120_FreeMediation") {
      exec (http ("Response_Free_Mediation_Get")
        .get ("/case/${claimId}/response/free-mediation")
        .check (CsrfCheck.save))
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  group("CMCDefRes_120_FreeMediationPost") {
        exec (http ("Response_Free_Mediation_Post")
          .post ("/case/${claimId}/mediation/free-mediation")
          .formParam (csrfParameter, csrfTemplate)
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val freeMediationDecision =
    group("CMCDefRes_120_FreeMediationDecision") {
     /* exec (http ("Response_Free_MediationYes")
        //.get("/case/38ea3269-a024-46f3-a6ca-03828f5f3da6/response/free-mediation")
        .get (currentPageTemplate))*/
    
        exec (http ("Response_Free_Mediation_Post")
          .post ("/case/${claimId}/mediation/how-mediation-works")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("mediationYes", "Continue with free mediation")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
    
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val freeMediationAgreement =
    group("CMCDefRes_130_FreeMediationAgreement") {
     /* exec (http ("Response_Free_MediationYes")
        //.get("/case/38ea3269-a024-46f3-a6ca-03828f5f3da6/response/free-mediation")
        .get (currentPageTemplate))*/
    
        exec (http ("Response_Free_Mediation_Post")
          .post ("/case/${claimId}/mediation/mediation-agreement")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("accept", "I agree")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
    
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  //expert can we use
  
  val mediationusage=
    group("CMCDefRes_140_MediationUsage") {
      /*exec (http ("Response_Free_Mediationusage")
        //.get("/case/38ea3269-a024-46f3-a6ca-03828f5f3da6/response/free-mediation")
        .get (currentPageTemplate))*/
    
        exec (http ("Response_Free_Mediation_Post")
          .post ("/case/${claimId}/mediation/can-we-use")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("accept", "I agree").formParam ("option", "no")
          .formParam ("mediationPhoneNumber", "07540657234")
         // .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val supportrequired=
    group("CMCDefRes_140_SupportRequiredGet") {
      exec (http ("support-required-Get")
        .get ("/case/${claimId}/directions-questionnaire/support-required")
        .check (CsrfCheck.save))
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
    .group("CMCDefRes_140_SupportRequiredGet") {
      exec (http ("Response_Free_Mediation_Post")
        .post ("/case/${caseId}/directions-questionnaire/support-required")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("signLanguageInterpreted", "")
        .formParam ("languageInterpreted", "")
        .formParam ("otherSupport", "")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        //	.check(regex("Free mediation"))

      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val hearinglocation=
    group("CMCDefRes_150_Hearinglocation") {
     /* exec (http ("hearinglocation")
        //.get("/case/38ea3269-a024-46f3-a6ca-03828f5f3da6/response/free-mediation")
        .get ("/case/${claimId}/directions-questionnaire/hearing-location"))*/
        
        exec (http ("Response_Free_Mediation_Post")
          .post ("/case/${caseId}/directions-questionnaire/hearing-location")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("courtAccepted", "yes")
          .formParam ("alternativePostcode", "")
          .formParam ("alternativeCourtName", "")
          .formParam ("courtName", "Central London County Court")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
    
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val expert=
    group("CMCDefRes_150_Expert") {
      
      
      exec (http ("Expert")
        .post ("/case/${claimId}/directions-questionnaire/expert")
        .formParam (csrfParameter, csrfTemplate)
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        //	.check(regex("Free mediation"))
      
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  
  
  val selfwitness=
    group("CMCDefRes_160_SelfWitness") {
      exec (http ("SelfWitness")
        .post ("/case/${caseId}/directions-questionnaire/self-witness")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("option", "no")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val otherwitness=
    group("CMCDefRes_170_OtherWitness") {
      exec (http ("Response_Free_Mediation_Post")
        .post ("/case/${caseId}/directions-questionnaire/other-witnesses")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("howMany", "")
        .formParam ("otherWitnesses", "no")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        //	.check(regex("Free mediation"))
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val hearingdates=
    group("CMCDefRes_180_HearingDates") {
      exec (http ("HearingDates")
        .post ("/case/${caseId}/directions-questionnaire/hearing-dates")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("hasUnavailableDates", "false")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val checkAndSend=
    group("CMCDefRes_190_CheckAndSend") {
      exec (http ("Response_CheckAndSend_Get")
        .get ("/case/${claimId}/response/check-and-send"))
    
        .exec (http ("Response_CheckAndSend_Post")
          .post ("/case/${claimId}/response/check-and-send")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("signed", "true")
          .formParam ("directionsQuestionnaireSigned", "true")
          .formParam ("type", "directions")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          .check (regex ("You’ve submitted your response"))
        )
        .pause (MinThinkTime seconds, MaxThinkTime seconds)
    }
  val cmcdefLogout =
    group("CMCDefRes_200_Logout") {
      exec (http ("Deflogout")
        .get ("/logout")
        .check (regex ("Sign in")))
    }
      
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
    
}
