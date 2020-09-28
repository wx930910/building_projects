/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.server.dns.messages;


import java.util.List;


/**
 * All communications inside of the domain protocol are carried in a single
 * format called a message.  The top level format of message is divided
 * into 5 sections (some of which are empty in certain cases) shown below:
 *
 *     +---------------------+
 *     |        Header       |
 *     +---------------------+
 *     |       Question      | the question for the name server
 *     +---------------------+
 *     |        Answer       | ResourceRecords answering the question
 *     +---------------------+
 *     |      Authority      | ResourceRecords pointing toward an authority
 *     +---------------------+
 *     |      Additional     | ResourceRecords holding additional information
 *     +---------------------+
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnsMessageModifier
{
    /**
     * The header section is always present.  The header includes fields that
     * specify which of the remaining sections are present, and also specify
     * whether the message is a query or a response, a standard query or some
     * other opcode, etc.
     */
    private int transactionId;
    private MessageType messageType;
    private OpCode opCode;
    private boolean authoritativeAnswer;
    private boolean truncated;
    private boolean recursionDesired;
    private boolean recursionAvailable;
    private boolean reserved;
    private boolean acceptNonAuthenticatedData;

    private ResponseCode responseCode;

    private List<QuestionRecord> questionRecords;
    private List<ResourceRecord> answerRecords;
    private List<ResourceRecord> authorityRecords;
    private List<ResourceRecord> additionalRecords;


    /**
     * Returns the {@link DnsMessage}.
     *
     * @return The {@link DnsMessage}.
     */
    public DnsMessage getDnsMessage()
    {
        return new DnsMessage( transactionId, messageType, opCode, authoritativeAnswer, truncated, recursionDesired,
            recursionAvailable, reserved, acceptNonAuthenticatedData, responseCode, questionRecords, answerRecords,
            authorityRecords, additionalRecords );
    }


    /**
     * @param acceptNonAuthenticatedData The acceptNonAuthenticatedData to set.
     */
    public void setAcceptNonAuthenticatedData( boolean acceptNonAuthenticatedData )
    {
        this.acceptNonAuthenticatedData = acceptNonAuthenticatedData;
    }


    /**
     * @param additionalRecords The additional to set.
     */
    public void setAdditionalRecords( List<ResourceRecord> additionalRecords )
    {
        this.additionalRecords = additionalRecords;
    }


    /**
     * @param answerRecords The answer to set.
     */
    public void setAnswerRecords( List<ResourceRecord> answerRecords )
    {
        this.answerRecords = answerRecords;
    }


    /**
     * @param authoritativeAnswer The authoritativeAnswer to set.
     */
    public void setAuthoritativeAnswer( boolean authoritativeAnswer )
    {
        this.authoritativeAnswer = authoritativeAnswer;
    }


    /**
     * @param authorityRecords The authority to set.
     */
    public void setAuthorityRecords( List<ResourceRecord> authorityRecords )
    {
        this.authorityRecords = authorityRecords;
    }


    /**
     * @param messageType The messageType to set.
     */
    public void setMessageType( MessageType messageType )
    {
        this.messageType = messageType;
    }


    /**
     * @param opCode The opCode to set.
     */
    public void setOpCode( OpCode opCode )
    {
        this.opCode = opCode;
    }


    /**
     * @param questionRecords The question to set.
     */
    public void setQuestionRecords( List<QuestionRecord> questionRecords )
    {
        this.questionRecords = questionRecords;
    }


    /**
     * @param recursionAvailable The recursionAvailable to set.
     */
    public void setRecursionAvailable( boolean recursionAvailable )
    {
        this.recursionAvailable = recursionAvailable;
    }


    /**
     * @param recursionDesired The recursionDesired to set.
     */
    public void setRecursionDesired( boolean recursionDesired )
    {
        this.recursionDesired = recursionDesired;
    }


    /**
     * @param reserved The reserved to set.
     */
    public void setReserved( boolean reserved )
    {
        this.reserved = reserved;
    }


    /**
     * @param responseCode The responseCode to set.
     */
    public void setResponseCode( ResponseCode responseCode )
    {
        this.responseCode = responseCode;
    }


    /**
     * @param transactionId The transactionId to set.
     */
    public void setTransactionId( int transactionId )
    {
        this.transactionId = transactionId;
    }


    /**
     * @param truncated The truncated to set.
     */
    public void setTruncated( boolean truncated )
    {
        this.truncated = truncated;
    }
}
