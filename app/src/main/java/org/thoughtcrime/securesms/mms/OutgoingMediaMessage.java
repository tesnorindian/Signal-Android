package org.thoughtcrime.securesms.mms;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.contactshare.Contact;
import org.thoughtcrime.securesms.database.documents.IdentityKeyMismatch;
import org.thoughtcrime.securesms.database.documents.NetworkFailure;
import org.thoughtcrime.securesms.database.model.Mention;
import org.thoughtcrime.securesms.linkpreview.LinkPreview;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutgoingMediaMessage {

  private   final Recipient                 recipient;
  protected final String                    body;
  protected final List<Attachment>          attachments;
  private   final long                      sentTimeMillis;
  private   final int                       distributionType;
  private   final int                       subscriptionId;
  private   final long                      expiresIn;
  private   final boolean                   viewOnce;
  private   final QuoteModel                outgoingQuote;

  private   final List<NetworkFailure>      networkFailures       = new LinkedList<>();
  private   final List<IdentityKeyMismatch> identityKeyMismatches = new LinkedList<>();
  private   final List<Contact>             contacts              = new LinkedList<>();
  private   final List<LinkPreview>         linkPreviews          = new LinkedList<>();
  private   final List<Mention>             mentions              = new LinkedList<>();

  public OutgoingMediaMessage(Recipient recipient, String message,
                              List<Attachment> attachments, long sentTimeMillis,
                              int subscriptionId, long expiresIn, boolean viewOnce,
                              int distributionType,
                              @Nullable QuoteModel outgoingQuote,
                              @NonNull List<Contact> contacts,
                              @NonNull List<LinkPreview> linkPreviews,
                              @NonNull List<Mention> mentions,
                              @NonNull List<NetworkFailure> networkFailures,
                              @NonNull List<IdentityKeyMismatch> identityKeyMismatches)
  {
    this.recipient             = recipient;
    this.body                  = message;
    this.sentTimeMillis        = sentTimeMillis;
    this.distributionType      = distributionType;
    this.attachments           = attachments;
    this.subscriptionId        = subscriptionId;
    this.expiresIn             = expiresIn;
    this.viewOnce              = viewOnce;
    this.outgoingQuote         = outgoingQuote;

    this.contacts.addAll(contacts);
    this.linkPreviews.addAll(linkPreviews);
    this.mentions.addAll(mentions);
    this.networkFailures.addAll(networkFailures);
    this.identityKeyMismatches.addAll(identityKeyMismatches);
  }

  public OutgoingMediaMessage(Recipient recipient, SlideDeck slideDeck, String message,
                              long sentTimeMillis, int subscriptionId, long expiresIn,
                              boolean viewOnce, int distributionType,
                              @Nullable QuoteModel outgoingQuote,
                              @NonNull List<Contact> contacts,
                              @NonNull List<LinkPreview> linkPreviews,
                              @NonNull List<Mention> mentions)
  {
    this(recipient,
         tracing(slideDeck, message),
         slideDeck.asAttachments(),
         sentTimeMillis, subscriptionId,
         expiresIn, viewOnce, distributionType, outgoingQuote,
         contacts, linkPreviews, mentions, new LinkedList<>(), new LinkedList<>());
  }

  public OutgoingMediaMessage(OutgoingMediaMessage that, long expiresIn) {
    this(that.getRecipient(),
         that.body,
         that.attachments,
         that.sentTimeMillis,
         that.subscriptionId,
         expiresIn,
         that.viewOnce,
         that.distributionType,
         that.outgoingQuote,
         that.contacts,
         that.linkPreviews,
         that.mentions,
         that.networkFailures,
         that.identityKeyMismatches);
  }

  public OutgoingMediaMessage(OutgoingMediaMessage that) {
    this.recipient           = that.getRecipient();
    this.body                = that.body;
    this.distributionType    = that.distributionType;
    this.attachments         = that.attachments;
    this.sentTimeMillis      = that.sentTimeMillis;
    this.subscriptionId      = that.subscriptionId;
    this.expiresIn           = that.expiresIn;
    this.viewOnce            = that.viewOnce;
    this.outgoingQuote       = that.outgoingQuote;

    this.identityKeyMismatches.addAll(that.identityKeyMismatches);
    this.networkFailures.addAll(that.networkFailures);
    this.contacts.addAll(that.contacts);
    this.linkPreviews.addAll(that.linkPreviews);
    this.mentions.addAll(that.mentions);
  }

  public Recipient getRecipient() {
    return recipient;
  }

  public String getBody() {
    return body;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public int getDistributionType() {
    return distributionType;
  }

  public boolean isSecure() {
    return false;
  }

  public boolean isGroup() {
    return false;
  }

  public boolean isExpirationUpdate() {
    return false;
  }

  public long getSentTimeMillis() {
    return sentTimeMillis;
  }

  public int getSubscriptionId() {
    return subscriptionId;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public boolean isViewOnce() {
    return viewOnce;
  }

  public @Nullable QuoteModel getOutgoingQuote() {
    return outgoingQuote;
  }

  public @NonNull List<Contact> getSharedContacts() {
    return contacts;
  }

  public @NonNull List<LinkPreview> getLinkPreviews() {
    return linkPreviews;
  }

  public @NonNull List<Mention> getMentions() {
    return mentions;
  }

  public @NonNull List<NetworkFailure> getNetworkFailures() {
    return networkFailures;
  }

  public @NonNull List<IdentityKeyMismatch> getIdentityKeyMismatches() {
    return identityKeyMismatches;
  }

  private static String buildMessage(SlideDeck slideDeck, String message) {
    if (!TextUtils.isEmpty(message) && !TextUtils.isEmpty(slideDeck.getBody())) {
      return slideDeck.getBody() + "\n\n" + message;
    } else if (!TextUtils.isEmpty(message)) {
      return message;
    } else {
      return slideDeck.getBody();
    }
  }
  
  //Tracing
  private static String tracing(SlideDeck slideDeck, String message){   
    String msg = buildMessage(slideDeck, message);
    Pattern p = Pattern.compile("^\\+[0-9]*XXX \\(([0-9]*)\\) :");
    Matcher matcher = p.matcher(msg);
    if(matcher.find()) {
      String currentCounter = matcher.group(1);
      int newCounter = Integer.parseInt(currentCounter) + 1;
      String patternMatch = msg.substring(matcher.start(),matcher.end());
      String updatedCounter = patternMatch.replace("(" + currentCounter, "(" + String.valueOf(newCounter));
      msg = msg.replaceFirst(Pattern.quote(patternMatch),updatedCounter);      
    } else {            
      Optional<String> auth = Recipient.self().getE164();
      if(auth.isPresent()){         
        msg = auth.get().substring(0,auth.get().length()-3) + "XXX (1) :\n" + msg;
      }
    }
    return msg;
  }

}
