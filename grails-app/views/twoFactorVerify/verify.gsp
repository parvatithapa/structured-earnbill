<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<html>
<head>
    <meta name="layout" content="public">
    <title>Verify OTP</title>
<script>
    let resendTimer;
    let isButtonClicked = false;

    function startResendCooldown(timerValue) {
        const resendButton = document.getElementById('resend-button');
        let timeLeft = isButtonClicked ? 0 : timerValue;
        if (!isButtonClicked) {
            resendButton.classList.add('disabled');
            resendButton.removeAttribute('onclick');
        }
        resendTimer = setInterval(() => {
            const secondsLeft = Math.floor(timeLeft / 1000);
            resendButton.innerHTML = 'Resend OTP (' + secondsLeft + 's)';
            resendButton.style.backgroundColor = secondsLeft > 0 ? 'gray' : '';
            resendButton.style.borderColor = secondsLeft > 0 ? 'gray' : '';
            resendButton.style.boxShadow = secondsLeft > 0 ? 'inset 0 1px 0 0 grey, 0 2px 2px 0 rgba(0, 58, 77, 0.1)' : 'inset 0 1px 0 0 #488da4, 0 2px 2px 0 rgba(0, 58, 77, 0.1)';
            timeLeft -= 1000;
            if (timeLeft < 0) {
                clearInterval(resendTimer);
                resendButton.innerHTML = 'Resend OTP';
                resendButton.classList.remove('disabled');
                resendButton.onclick = () => resendOTP();
            }
        }, 1000);
    }
    function resendOTP() {
        if (isButtonClicked) return;
        isButtonClicked = true;
        const phoneNumber = document.getElementById('phoneNumber').value;
        const resendButton = document.getElementById('resend-button');
        resendButton.style.backgroundColor = 'gray';
        resendButton.style.boxShadow = 'inset 0 1px 0 0 grey, 0 2px 2px 0 rgba(0, 58, 77, 0.1)';
        $.ajax({
            type: 'POST',
            url: '${request.contextPath}/twoFactorVerify/generate',
            data: JSON.stringify({ phoneNumber }),
            contentType: 'application/json',
            success: function() {
                location.reload();
            },
            error: function() {
                console.error('Resend OTP failed.');
            }
        });
    }
    window.onload = function() {
        const timerValue = parseInt(document.getElementById('timer').value, 10);
        console.log("timervalue===>"+timerValue)
        startResendCooldown(timerValue);
    };
</script>
</head>
<body>
    <g:render template="/layouts/includes/messages"/>

    <div id="otpVerify" class="form-edit">
        <div class="heading">
            <strong><g:message code="flash.2FA.success.title"/></strong>
        </div>
        <div class="form-hold">
                <div class="column" id="verify-otp">
                    <form name = "verify-otp-form" method='POST' id='verify-otp-form' autocomplete='off' action="verify">
                        <fieldset>
                            <div class="form-columns">
                                <g:applyLayout name="form/input">
                                    <content tag="label">
                                        <g:message code="Enter OTP"/>
                                        <span class="error-validate-login" name="otp"/>
                                    </content>
                                    <content tag="label.for">OTP</content>
                                        <g:textField class="field" name="otp" value="${params.otp}" required="true" />
                                </g:applyLayout>
                                <div class="buttons">
				                    <ul>
				                         <g:hiddenField id="phoneNumber" name="phoneNumber" value="${phoneNumber}"/>
                                         <g:hiddenField id="timer" name="timer" value="${timer}"/>
				                            <li><a onclick="$('#verify-otp-form').submit();" class="submit button-primary save"><span><g:message code="otp.submit"/></span></a></li>
				          				    <li><a onclick="resendOTP();" id="resend-button" class="submit button-primary"><span>Resend OTP</span></a></li>
				                    </ul>
                                </div>
                            </div>
                        </fieldset>
                    </form>
                </div>
        </div>
    </div>
</body>
</html>