
package uk.gov.hmcts.reform.cmc.performance.scenarios
import io.gatling.core.Predef._
      import io.gatling.http.Predef._
      import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment}
      import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CsrfCheck.{csrfParameter, csrfTemplate}
      import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CurrentPageCheck.currentPageTemplate
      
      import scala.concurrent.duration._
      
object CMC_Claimant {

val BaseURL = Environment.baseURL
val IdAMURL = Environment.idamURL
val MinThinkTime = Environment.minThinkTime
val MaxThinkTime = Environment.maxThinkTime
val CommonHeader = Environment.commonHeader
val paymentURL = Environment.PaymentURL
val TotalAmount = scala.util.Random.nextInt(9999)
  // below request for access ing cmc homepage
  val home =
      
      group ("CMC_010_Homepage") {
      exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
      .exec (http ("Homepage")
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
      exec (http ("SubmitLogin")
      .post ("${currentPage}")
      .formParam("username", "${email}")
      .formParam("password", "${password}")
      .formParam ("_csrf", "${csrf}")
      .check(regex("Claims you’ve made").optional.saveAs("existingclaimcheck"))
      )
      // .check (regex ("Find out if you can make a claim using this service")))
      }.pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      //below are eligibility related questions
  
      val eligibility =
      doIf("${existingclaimcheck.exists()}") {//to check if the have existing claims
      group ("CMC_030_FindoutEligibilityPage") {
      exec (http ("FindoutEligibilityPage")
      .get ("/eligibility")
      .check (regex ("Find out if you can make a claim using this service")))
      }
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("CMC_040__Eligibility_TotalAmountYouAreclaiming_GET") {
      exec (http ("TotalAmountYouAreclaiming_GET")
      .get ("/eligibility/claim-value")
      .check (CsrfCheck.save)
      .check (regex ("Total amount you’re claiming")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX04_CMC_Eligibility_TotalAmountYouAreclaiming_POST") {
      exec (http ("TX04_CMC_Eligibility_TotalAmountYouAreclaiming_POST")
      .post ("/eligibility/claim-value")
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("claimValue", "UNDER_10000")
      .check (CurrentPageCheck.save)
      .check (CsrfCheck.save))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      //eligibility questions : is the Defendent  Single or organisation
      .group ("TX06_CMC_Eligibility_SingleDefendant") {
      exec (http ("TX06_CMC_Eligibility_SingleDefendant")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("singleDefendant", "no")
      .check (CurrentPageCheck.save)
      .check (CsrfCheck.save)
      .check (regex ("Does the person or organisation you’re claiming against have a postal address in England or Wales?")))
      } //
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      // below is the get address
      .group ("TX07_CMC_Eligibility_DefendantAddress") {
      exec (http ("TX07_CMC_Eligibility_DefendantAddress")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("defendantAddress", "yes")
      .check (CurrentPageCheck.save)
      .check (CsrfCheck.save)
      .check (regex ("Who are you making the claim for?")))
      }
      //.exitHereIfFailed//Are you 18 or over?
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      //get claim type
      /*http("request_268")
      .get("/eligibility/claim-type")
      .headers(headers_202),*/
      
      .group ("TX08_CMC_Eligibility_ClaimType") {
      exec (http ("TX08_CMC_Eligibility_ClaimType")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("claimType", "PERSONAL_CLAIM")
      .check (CurrentPageCheck.save)
      .check (CsrfCheck.save)
      .check (regex ("Do you have a postal address in the UK?")))
      }
      
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX09_CMC_Eligibility_ClaimantAddress") {
      exec (http ("TX09_CMC_Eligibility_ClaimantAddress")
      .post (currentPageTemplate).formParam (csrfParameter, csrfTemplate)
      .formParam ("claimantAddress", "yes")
      .check (CurrentPageCheck.save)
      .check (CsrfCheck.save)
      .check (regex ("Is your claim for a tenancy deposit?")))
      }
      
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX010_CMC_Eligibility_TenancyDeposit") {
      exec (http ("TX010_CMC_Eligibility_TenancyDeposit")
      .post ("/eligibility/claim-is-for-tenancy-deposit")
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("claimIsForTenancyDeposit", "no")
      .check (CurrentPageCheck.save)
      .check (regex ("Are you claiming against a government department?")))
      }
      // .exitHereIfFailed
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX011_CMC_Eligibility_GovernmentDepartment") {
      exec (http ("TX011_CMC_Eligibility_GovernmentDepartment")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("governmentDepartment", "no")
      .check (CurrentPageCheck.save)
      .check (CsrfCheck.save)
      .check (regex ("Do you believe the person you’re claiming against is 18 or over?")))
      }
      
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX012_CMC_Eligibility_DefendantAge") {
      exec (http ("TX012_CMC_Eligibility_DefendantAge")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("defendantAge", "yes")
      .check (CurrentPageCheck.save)
      .check (CsrfCheck.save)
      .check (regex ("Are you 18 or over?")))
      }
      // .exitHereIfFailed//Do you believe the person you’re claiming against is 18 or over?
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX013_CMC_Eligibility_Over18") {
      exec (http ("TX013_CMC_Eligibility_Over18")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("eighteenOrOver", "yes")
      .check (CurrentPageCheck.save).check (CsrfCheck.save))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      //below is the help with fee request
      .group ("TX013_CMC_Eligibility_HelpwithFee") {
      exec (http ("TX013_CMC_Eligibility_HelpwithFee")
      .post ("/eligibility/help-with-fees")
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("helpWithFees", "no")
      .check (CurrentPageCheck.save))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      /*
      resolving dispute related requests
      */
      val resolvingDispute =
      group ("TX015_CMC_ResolvingThisDispute_ResolvingThisDispute-GET") {
      exec(http ("TX015_CMC_ResolvingThisDispute_ResolvingThisDispute-GET")
      .get ("/claim/resolving-this-dispute")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Try to resolve the dispute")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX016_CMC_ResolvingThisDispute_ResolvingThisDispute-POST") {
      exec (http ("TX016_CMC_ResolvingThisDispute_ResolvingThisDispute-POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .check (regex ("Prepare your claim")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      /*
      below requests are for completing your claim
      */
      
      val completingClaim =
      group ("TX017_CMC_Completingclaim_CompletingClaim-GET") {
      exec (http ("TX017_CMC_Completingclaim_CompletingClaim-GET")
      .get ("/claim/completing-claim")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Get the details right")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      .group ("TX018_CMC_Completingclaim_CompletingClaim-POST") {
      exec (http ("TX018_CMC_Completingclaim_CompletingClaim-POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .check (regex ("Prepare your claim")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      /*
      below requests are for your details-party type selection
      */
      val yourDetails =
      group ("TX019_CMC_YourDetail_PartyTypeSelection-GET") {
      exec (http ("TX019_CMC_YourDetail_PartyTypeSelection-GET")
      .get ("/claim/claimant-party-type-selection")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("About you and this claim")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      /*
      below requests are for your details-party type selection
      */
      .group ("TX020_CMC_YourDetail_PartyTypeSelection-POST") {
      exec (http ("TX020_CMC_YourDetail_PartyTypeSelection-POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("type", "individual")
      .formParam ("saveAndContinue", "Save and continue").check (CsrfCheck.save).check (CurrentPageCheck.save).check (regex ("your details")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      // following are your details
      .group ("TX021_CMC_YourDetail_YourDetails-POST") {
      exec (http ("TX021_CMC_YourDetail_YourDetails-POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("name", "mr vijay kumar")
      .formParam ("address[postcodeLookup]", "TW3 3SD")
      .formParam ("address[addressList]", """{"addressLines":["20, HIBERNIA GARDENS","",""],"townOrCity":"HOUNSLOW","postCode":"TW3 3SD"}""")
      .formParam ("address[line1]", "20, HIBERNIA GARDENS")
      .formParam ("address[line2]", "")
      .formParam ("address[line3]", "").formParam ("address[city]", "HOUNSLOW")
      .formParam ("address[postcode]", "TW3 3SD")
      .formParam ("address[addressVisible]", "true")
      .formParam ("address[addressSelectorVisible]", "false")     .formParam ("address[enterManually]", "false")
      .formParam ("hasCorrespondenceAddress", "false")
      .formParam ("correspondenceAddress[postcodeLookup]", "").formParam ("correspondenceAddress[addressList]", "")
      .formParam ("correspondenceAddress[line1]", "")
      .formParam ("correspondenceAddress[line2]", "")
      .formParam ("correspondenceAddress[line3]", "")
      .formParam ("correspondenceAddress[city]", "")
      .formParam ("correspondenceAddress[postcode]", "")
      .formParam ("correspondenceAddress[addressVisible]", "true")  .formParam ("correspondenceAddress[addressSelectorVisible]", "false")
      .formParam ("correspondenceAddress[enterManually]", "false").formParam ("saveAndContinue", "Save and continue")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("What is your date of birth")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX022_CMC_YourDetail_DateOfBirth-POST") {
      exec (http ("TX022_CMC_YourDetail_DateOfBirth-POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("known", "true")
      .formParam ("date[day]", "31")
      .formParam ("date[month]", "3")
      .formParam ("date[year]", "1980")
      .formParam ("saveAndContinue", "Save and continue")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Enter a phone number")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX023_CMC_YourDetail_AddPhoneNumber-POST") {
      exec (http ("TX023_CMC_YourDetail_AddPhoneNumber-POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("number", "07123456789")
      .formParam ("saveAndContinue", "Save and continue")
      .check (regex ("Their details")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      /*
      Their details related requests
      */
      
      val TheirDetails =
      group ("TX024_CMC_TheirDetail_PartyTypeSelection_GET") {
      exec (http ("TX024_CMC_TheirDetail_PartyTypeSelection_GET")
      .get ("/claim/defendant-party-type-selection")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Who are you making the claim against")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX025_CMC_TheirDetail_PartyTypeSelection_POST") {
      exec (http ("TX025_CMC_TheirDetail_PartyTypeSelection_POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("type", "company")
      .check (CsrfCheck.save).check (CurrentPageCheck.save))
      }
      //.check(regex("Enter the defendant’s details")))
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX026_CMC_TheirDetail_TheirDetails_POST") {
      exec (http ("TX026_CMC_TheirDetail_TheirDetails_POST")
      .post (currentPageTemplate).formParam (csrfParameter, csrfTemplate)
      .formParam ("name", "perftest holiday company")
      .formParam ("contactPerson", "asasasas")
      .formParam ("address[postcodeLookup]", "TW3 3SD")
      .formParam ("address[addressList]", """{"addressLines":["10, HIBERNIA GARDENS","",""],"townOrCity":"HOUNSLOW","postCode":"TW3 3SD"}""")
      .formParam ("address[line1]", "10, HIBERNIA GARDENS")
      .formParam ("address[line2]", "")
      .formParam ("address[line3]", "")
      .formParam ("address[city]", "HOUNSLOW")
      .formParam ("address[postcode]", "TW3 3SD")
      .formParam ("address[addressVisible]", "true")
      .formParam ("address[addressSelectorVisible]", "false").formParam ("address[enterManually]", "false")
      .formParam ("saveAndContinue", "Save and continue")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save).check (regex ("Their email address")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX027_CMC_TheirDetail_EmailAddress_POST") {
      exec (http ("TX027_CMC_TheirDetail_EmailAddress_POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("address", "${defemail}")
      .formParam ("saveAndContinue", "Save and continue")
      .check (regex ("Their phone number"))
      )
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      .group ("TX027_CMC_TheirDetail_Mobile_POST") {
      exec (http ("TX027_CMC_TheirDetail_Mobile_POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("number", "07540612043")
      .formParam ("saveAndContinue", "Save and continue")
      //  .check(regex("Make a money claim"))
      )
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      /*
      following requests are for amount
      */
      
      val amount =
      group ("TX028_CMC_ClaimAmout_ClaimAmount_GET") {
      exec (http ("TX028_CMC_ClaimAmout_ClaimAmount_GET")
      .get ("/claim/amount")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Your claim could be for a single amount or made up of multiple items")))
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      .exec (http ("TX029_CMC_ClaimAmout_ClaimAmount_POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("rows[0][reason]", "Performance test cmc on sprod")
      .formParam ("rows[0][amount]", TotalAmount)
      .formParam ("rows[1][reason]", "")
      .formParam ("rows[1][amount]", "")
      .formParam ("rows[2][reason]", "")
      .formParam ("rows[2][amount]", "")
      .formParam ("rows[3][reason]", "")
      .formParam ("rows[3][amount]", "")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Do you want to claim interest")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      .group ("TX030_CMC_ClaimAmout_Interest_POST") {
      exec (http ("TX030_CMC_ClaimAmout_Interest_POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("option", "no")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX031_CMC_ClaimAmout_HelpwithFee_POST") {
      exec (http ("TX031_CMC_ClaimAmout_HelpwithFee_POST")
      .post ("/claim/help-with-fees")
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("helpWithFeesNumber", "")
      .formParam ("declared", "no")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group("TX031_CMC_ClaimAmout_TotalAmount_POST") {
      exec (http ("TX031_CMC_ClaimAmout_TotalAmount_POST")
      .post (currentPageTemplate)
      .formParam (csrfParameter, csrfTemplate)
      .check (regex ("Claim details"))
      )
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      /*
      following requesrs are related to reason
      */
      
      val reason =
      group ("TX032_CMC_ClaimDetail_Reason_GET") {
      exec (http ("TX032_CMC_ClaimDetail_Reason_GET")
      .get ("/claim/reason")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Briefly explain your claim")))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX033_CMC_ClaimDetail_Reason_POST") {
      exec (http ("TX033_CMC_ClaimDetail_Reason_POST")
      .post (currentPageTemplate)
      .formParam ("_csrf", "${csrf}")
      .formParam ("reason", """sjdhsjdshdsdsjdksjdkjskdjskd
      sdsdsdsdjsjdskjdksjdksjd
      sdsjdskdjksjdksjdksjdksjdksjd""")
      .check (CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Timeline of events"))
      )
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX034_CMC_ClaimDetail_Timeline_POST") {
      exec (http ("TX034_CMC_ClaimDetail_Timeline_POST")
      .post ("/claim/timeline")
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("rows[0][date]", "13 June 2020")
      .formParam ("rows[0][description]", "gfhfhgjghjfghkjhkgjdgfhgfjd")
      .formParam ("rows[1][date]", "16 June 2020")
      .formParam ("rows[1][description]", "hdfgdgjdghjdghjjdffg")
      .formParam ("rows[2][date]", "")
      .formParam ("rows[2][description]", "")
      .formParam ("rows[3][date]", "")
      .formParam ("rows[3][description]", "")
      //.check(CsrfCheck.save)
      .check (CurrentPageCheck.save)
      .check (regex ("Evidence"))
      )
      
      .exec (http ("TX035_CMC_ClaimDetail_ClaimEvidence")
      .get ("/claim/evidence")
      .check (CsrfCheck.save))
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      .group ("TX035_CMC_ClaimDetail_Timeline_Evidence_POST") {
      exec (http ("TX035_CMC_ClaimDetail_Timeline_Evidence_POST")
      .post ("/claim/evidence")
      .formParam (csrfParameter, csrfTemplate)
      .formParam ("rows[0][type]", "PHOTO")
      .formParam ("rows[0][description]", "dfsdfgdsgsdgfdsf")
      .formParam ("rows[1][type]", "RECEIPTS")
      .formParam ("rows[1][description]", "vbvbvbvbvbvbvbv")
      .formParam ("rows[2][type]", "")
      .formParam ("rows[2][description]", "")
      .formParam ("rows[3][type]", "")
      .formParam ("rows[3][description]", "")
      // .check(regex("Check and submit your claim"))
      )
      }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
      
      /*
      following requests are for check and send including payments
      */
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
      .check (css ("input[name='csrfToken']", "value").saveAs ("_csrfTokenCardDetailPage"))
      .check (css ("input[name='chargeId']", "value").saveAs ("CardDetailPageChargeId"))
      //.check(regex("""/card_details/(.+)',""").saveAs("_csrfCardDetailPageChargeId"))
      .check (regex ("Enter card details"))
      )
      .exec (http ("TX038_CMC_CardDetail_CheckCardDetail")
      .post (paymentURL + "/check_card/${CardDetailPageChargeId}")
      //DG commented out line below for debugging
        .formParam ("cardNo", "4444333322221111")
      .headers (Environment.headers_996)
      )
      }
      
      
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
      
      .group("TX038_CMC_CardDetail_SubmitCardDetail") {
      exec (http ("TX038_CMC_CardDetail_SubmitCardDetail")
      .post (paymentURL + "/card_details/${CardDetailPageChargeId}")
       // .post (paymentURL + "/check_card/${CardDetailPageChargeId}")
        .formParam ("chargeId", "${CardDetailPageChargeId}")
      .formParam ("csrfToken", "${_csrfTokenCardDetailPage}")
        //DG commented out line below for debugging
      .formParam ("cardNo", "4444333322221111")
      .formParam ("expiryMonth", "10")
      .formParam ("expiryYear", "24")
      .formParam ("cardholderName", "fgdfdf")
      .formParam ("cvc", "123")
      .formParam ("addressCountry", "GB")
      .formParam ("addressLine1", "4")
      .formParam ("addressLine2", "Hibernia Gardens")
      .formParam ("addressCity", "Hounslow")
      .formParam ("addressPostcode", "TW3 3SD")
      .formParam ("email", "vijaykanth6@gmail.com")
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
      
      /*.exec {
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
      
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
      }
