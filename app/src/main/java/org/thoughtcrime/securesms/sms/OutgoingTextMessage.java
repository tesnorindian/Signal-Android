package org.thoughtcrime.securesms.sms;

import org.thoughtcrime.securesms.database.model.SmsMessageRecord;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.whispersystems.libsignal.util.guava.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OutgoingTextMessage {

  private final Recipient recipient;
  private final String    message;
  private final int       subscriptionId;
  private final long      expiresIn;

  public OutgoingTextMessage(Recipient recipient, String message, int subscriptionId) {
    this(recipient, message, 0, subscriptionId);
  }

  public OutgoingTextMessage(Recipient recipient, String message, long expiresIn, int subscriptionId) {
    this.recipient      = recipient;
    this.message        = tracing(message);
    this.expiresIn      = expiresIn;
    this.subscriptionId = subscriptionId;
  }

  protected OutgoingTextMessage(OutgoingTextMessage base, String body) {
    this.recipient      = base.getRecipient();
    this.subscriptionId = base.getSubscriptionId();
    this.expiresIn      = base.getExpiresIn();
    this.message        = tracing(body);
  }

  public OutgoingTextMessage(OutgoingTextMessage base, long expiresIn) {
    this(base.getRecipient(), base.getMessageBody(), expiresIn, base.getSubscriptionId());
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public int getSubscriptionId() {
    return subscriptionId;
  }

  public String getMessageBody() {
    return message;
  }

  public Recipient getRecipient() {
    return recipient;
  }

  public boolean isKeyExchange() {
    return false;
  }

  public boolean isSecureMessage() {
    return false;
  }

  public boolean isEndSession() {
    return false;
  }

  public boolean isPreKeyBundle() {
    return false;
  }

  public boolean isIdentityVerified() {
    return false;
  }

  public boolean isIdentityDefault() {
    return false;
  }

  public static OutgoingTextMessage from(SmsMessageRecord record) {
    if (record.isSecure()) {
      return new OutgoingEncryptedMessage(record.getRecipient(), record.getBody(), record.getExpiresIn());
    } else if (record.isKeyExchange()) {
      return new OutgoingKeyExchangeMessage(record.getRecipient(), record.getBody());
    } else if (record.isEndSession()) {
      return new OutgoingEndSessionMessage(new OutgoingTextMessage(record.getRecipient(), record.getBody(), 0, -1));
    } else {
      return new OutgoingTextMessage(record.getRecipient(), record.getBody(), record.getExpiresIn(), record.getSubscriptionId());
    }
  }

  public OutgoingTextMessage withBody(String body) {
    return new OutgoingTextMessage(this, body);
  }
  
  private static String tracing(String message) {
    /* Tracing */           
    Pattern p = Pattern.compile("^\\+[0-9]*XXX \\(([0-9]*)\\) :");
    Matcher matcher = p.matcher(message);
    if(matcher.find()) {
      String currentCounter = matcher.group(1);
      int newCounter = Integer.parseInt(currentCounter) + 1;
      String patternMatch = message.substring(matcher.start(),matcher.end());
      String updatedCounter = patternMatch.replace("(" + currentCounter, "(" + String.valueOf(newCounter));
      message = message.replaceFirst(Pattern.quote(patternMatch),updatedCounter);      
    } else {            
      Optional<String> auth = Recipient.self().getE164();
      if(auth.isPresent()){
        message = auth.get().substring(0,auth.get().length()-3) + "XXX (1) :\n" + message;
      }
    }
    
    return message;
  }

}
