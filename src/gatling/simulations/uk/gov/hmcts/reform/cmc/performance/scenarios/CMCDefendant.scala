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
  
  /*
  //below journey is to link the claims to defendants
  //End of the journey, we are linking claim number, security code to defenddant user so that when defendant login he can see the claim
   */
  
  val landingPage =
    group("CMCLinkClaim_010_LandingPage_Get") {
      exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
        .exec (http ("LandingPage_Get")
        .get ("/first-contact/start")
          .headers(Environment.headers_firstcontact)
        .check (CsrfCheck.save)
            .check(status.in(200,201,204))
        .check (regex ("Start now")))
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val startPage=
    group("CMCLinkClaim_020_LandingPage_Post") {
      exec (http ("LandingPage_Post")
        .post ("/first-contact/start")
          .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("start-button", "Start now")
        .check(status.in(200,201,204))
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Enter your claim number"))
      )
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val claimNumber =
    group("CMCLinkClaim_030_ClaimNumber") {
      exec (http ("ClaimNumber")
        .post (currentPageTemplate)
        .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("reference", "${claimno}")
        .check(css("input[name='redirect_uri']", "value").saveAs("redirectUri"))
        .check(css("input[name='client_id']", "value").saveAs("clientId"))
        .check(css("input[name='state']", "value").saveAs("state"))
        .check(status.in(200,201,204))
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        .check (regex ("Enter security code"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val enterpinPost=
    group("CMCLinkClaim_040_Pin"){
      exec (http ("Def_PinPost")
        .post (IdAMURL+"/loginWithPin")
       // .post (currentPageTemplate)
        .headers(Environment.headers_withpin)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("pin","${pin}")
        .formParam ("redirect_uri","${redirectUri}")
        .formParam ("client_id","${clientId}")
        .formParam ("state","${state}")
        .check(status.in(200,201,204))
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Claim details"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  
  val ClaimSummary =
    group("CMCLinkClaim_050_ClaimSummary") {
      exec (http ("ClaimSummary")
        .post (currentPageTemplate)
          .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .check(status.in(200,201,204))
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
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  /*========================================================================================
  // below are the defendant response details
  =========================================================================================*/
  
  val dashboard =
    exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
      .group ("CMCDefRes_010_Dashboard") {
        exec (http ("Dashboard")
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
      .check(status.in(200,201,204))
      .check(regex("Claims made against you"))
     .check(regex("""<a href="/dashboard/(.+)/defendant"""").find(6).optional.saveAs("claimId"))
    ).exitHereIfFailed
  }.pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  //following commented code will be used to get the claim count and so not removing this
  
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
        .check(status.in(200,201,204))
        //.check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
      
        .group("CMCDefRes_040_YourDetailsGet") {
        exec (http ("YourDetails_Get")
          .get ("/case/${claimId}/response/your-details")
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
        )
      }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val defendantDetails=
    group("CMCDefRes_050_YourDetailsPost") {
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
    group("CMCDefRes_060_DOB") {
      exec (http ("Response_DOB")
        .post (currentPageTemplate)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("known", "true")
        .formParam ("date[day]", "01")
        .formParam ("date[month]", "08")
        .formParam ("date[year]", "1978")
        .formParam ("saveAndContinue", "Save and continue")
        .check(status.in(200,201,204))
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        // .check(regex("Add a contact number"))
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val mobile=
    group("CMCDefRes_070_Mobile") {
      exec (http ("Response_Mobile")
        .post (currentPageTemplate)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("number", "07548723412")
        .formParam ("saveAndContinue", "Save and continue")
        .check(status.in(200,201,204))
        //.check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  //need to add this here-https://moneyclaim.nonprod.platform.hmcts.net/case/6e99f589-5a25-4abf-976a-49622380d6bf/response/task-list
  
  val moreTimeRequest=
    group("CMCDefRes_080_MoreTimeRequestGet") {
      exec (http ("Response_MoreTimeRequest_Get")
        .get ("/case/${claimId}/response/more-time-request")
        .check(status.in(200,201,204))
        .check (CsrfCheck.save)
      )
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
    .group("CMCDefRes_090_MoreTimeRequestGet") {
        exec (http ("Response_MoreTimeRequest_Post")
          .post ("/case/${claimId}/response/more-time-request")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("option", "no")
          .formParam ("saveAndContinue", "Save and continue")
          .check(status.in(200,201,204))
          .check (CurrentPageCheck.save)
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  val responseType=
    group("CMCDefRes_100_ResponseType") {
      exec (http ("Response_ResponseType_Get")
        .get("/case/${claimId}/response/response-type")
        .check(status.in(200,201,204))
        .check (CsrfCheck.save))
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
      
        .group("CMCDefRes_110_ResponseType")
        {
        exec (http ("ResponseType_Post")
          .post ("/case/${claimId}/response/response-type")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("type[value]", "DEFENCE")
          .formParam ("saveAndContinue", "Save and continue")
          .check(status.in(200,201,204))
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val reject_All_Claims=
    group("CMCDefRes_120_RejectAllClaim") {
        exec (http ("Reject_All_Of_Claim_Post")
          .post ("/case/${claimId}/response/reject-all-of-claim")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("option", "dispute")
          .formParam ("saveAndContinue", "Save and continue")
          .check(status.in(200,201,204))
          .check (CurrentPageCheck.save)
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val yourDefence=
    group("CMCDefRes_130_YourDefence") {
      exec (http ("Your_Defence_Post_Get")
        .get ("/case/${claimId}/response/your-defence")
        .check(status.in(200,201,204))
        .check (CsrfCheck.save))
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
       // .get (currentPageTemplate))
    .group("CMCDefRes_140_YourDefencePost"){
        exec (http ("Defence_Post")
          .post ("/case/${claimId}/response/your-defence")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("text", "adadasdfadad")
          .formParam ("saveAndContinue", "Save and continue")
          .check(status.in(200,201,204))
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //.check(regex("Why do you disagree with the claim?"))
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val timeLine=
    group("CMCDefRes_150_Timeline") {
      exec (http ("Response_TimeLine")
        .post ("/case/${claimId}/response/timeline")
        .formParam (csrfParameter, csrfTemplate)
        .formParam("rows[0][date]", "01 June 2019")
        .formParam("rows[0][description]", "asasasasasas")
        .formParam("rows[1][date]", "01 Sep 2019")
        .formParam("rows[1][description]", "sdsdsdsdsdsd")
        .formParam("rows[2][date]", "")
        .formParam("rows[2][description]", "")
        .formParam("rows[3][date]", "")
        .formParam("rows[3][description]", "")
        .formParam("comment", "")
        .formParam ("saveAndContinue", "Save and continue")
        .check(status.in(200,201,204))
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val evidence=
      group("CMCDefRes_160_ResponseEvidence") {
      exec (http ("Response_Evidence")
        .post ("/case/${claimId}/response/evidence")
        .formParam (csrfParameter, csrfTemplate)
        .formParam("rows[0][type]", "PHOTO")
        .formParam("rows[0][description]", "asasasasas")
        .formParam("rows[1][type]", "RECEIPTS")
        .formParam("rows[1][description]", "sdsdsdsd")
        .formParam("rows[2][type]", "")
        .formParam("rows[2][description]", "")
        .formParam("rows[3][type]", "")
        .formParam("rows[3][description]", "")
        .formParam("comment", "")
        .formParam ("saveAndContinue", "Save and continue")
        .check(status.in(200,201,204))
       // .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  
  val freeMediation =
    group("CMCDefRes_170_FreeMediation") {
      exec (http ("Free_Mediation_Get")
        .get ("/case/${claimId}/mediation/free-mediation")
        .check(status.in(200,201,204))
        .check (CsrfCheck.save))
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
    .group("CMCDefRes_180_FreeMediationPost") {
        exec (http ("Free_Mediation_Post")
          .post ("/case/${claimId}/mediation/free-mediation")
          .formParam (csrfParameter, csrfTemplate)
          .check(status.in(200,201,204))
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val freeMediationDecision =
    group("CMCDefRes_190_FreeMediationDecision") {
        exec (http ("Free_Mediation_Post")
          .post ("/case/${claimId}/mediation/how-mediation-works")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("mediationYes", "Continue with free mediation")
          .check(status.in(200,201,204))
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val freeMediationAgreement =
    group("CMCDefRes_200_FreeMediationAgreement") {
        exec (http ("Free_Mediation_Agreement")
          .post ("/case/${claimId}/mediation/mediation-agreement")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("accept", "I agree")
          .check(status.in(200,201,204))
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
    
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  //expert can we use
  
  val mediationusage=
    group("CMCDefRes_210_MediationUsage") {
        exec (http ("Free_MediationUsage")
          .post ("/case/${claimId}/mediation/can-we-use")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("accept", "I agree").formParam ("option", "no")
          .formParam ("mediationPhoneNumber", "07540657234")
          .check(status.in(200,201,204))
          .check (CurrentPageCheck.save)
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val supportrequired=
    group("CMCDefRes_220_SupportRequiredGet") {
      exec (http ("support-required-Get")
        .get ("/case/${claimId}/directions-questionnaire/support-required")
        .check(status.in(200,201,204))
        .check (CsrfCheck.save))
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
    .group("CMCDefRes_230_SupportRequiredPost") {
      exec (http ("SupportRequiredPost")
        .post ("/case/${claimId}/directions-questionnaire/support-required")
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
    group("CMCDefRes_240_Hearinglocation") {
        exec (http ("Hearinglocation")
          .post ("/case/${claimId}/directions-questionnaire/hearing-location")
          .formParam (csrfParameter, csrfTemplate)
          .formParam ("courtAccepted", "yes")
          .formParam ("alternativePostcode", "")
          .formParam ("alternativeCourtName", "")
          .formParam ("courtName", "Central London County Court")
          .check(status.in(200,201,204))
          .check (CsrfCheck.save)
          .check (CurrentPageCheck.save)
          //	.check(regex("Free mediation"))
    
        )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val expert=
    group("CMCDefRes_250_Expert") {
      exec (http ("Expert")
        .post ("/case/${claimId}/directions-questionnaire/expert")
        .check(status.in(200,201,204))
        .formParam (csrfParameter, csrfTemplate)
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  val selfwitness=
    group("CMCDefRes_260_SelfWitness") {
      exec (http ("SelfWitness")
        .post ("/case/${claimId}/directions-questionnaire/self-witness")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("option", "no")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val otherwitness=
    group("CMCDefRes_270_OtherWitness") {
      exec (http ("OtherWitness")
        .post ("/case/${claimId}/directions-questionnaire/other-witnesses")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("howMany", "")
        .formParam ("otherWitnesses", "no")
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val hearingdates=
    group("CMCDefRes_280_HearingDates") {
      exec (http ("HearingDates")
      .post ("/case/${claimId}/directions-questionnaire/hearing-dates")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("hasUnavailableDates", "false")
        //.check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
      )
    }
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
  val checkAndSend=
    group("CMCDefRes_290_CheckAndSendGet1") {
      exec (http ("CheckAndSend_Get1")
        .get ("/case/${claimId}/response/check-and-send")
        .check(status.in(200,201,204)))
    }
        .pause(MinThinkTime seconds, MaxThinkTime seconds)
  
    .group("CMCDefRes_300_CheckAndSendGet2") {
    exec (http ("CheckAndSend_Get2")
      .get ("/case/${claimId}/response/check-and-send")
      .check(status.in(200,201,204))
      .check (CsrfCheck.save))
  }
    .pause(MinThinkTime seconds, MaxThinkTime seconds)
    .group("CMCDefRes_310_CheckAndSendPost") {
      exec (http ("CheckAndSend_Post")
        .post ("/case/${claimId}/response/check-and-send")
         // .headers(Environment.headers_checkAndSend)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("signed", "true")
        .formParam ("directionsQuestionnaireSigned", "true")
        .formParam ("type", "directions")
        .check (regex ("You’ve submitted your response"))
      )
    }
        .pause (MinThinkTime seconds, MaxThinkTime seconds)
    
  val cmcdefLogout =
    group("CMCDefRes_320_Logout") {
      exec (http ("Deflogout")
        .get ("/logout")
        .check (regex ("Sign in")))
    }
      
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
    
}
