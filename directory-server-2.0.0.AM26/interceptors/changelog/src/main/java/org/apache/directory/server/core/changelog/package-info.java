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

/**
 * <h1>Ideas</h1>
 * <p>
 * Use AdminModel to control what goes into the changelog ?  Or use admin model to identify 
 * scopes/concerns which are put into different channels in the changelog?  Cool idea perhaps,
 * perhaps not.  To some degree the Dn does the scope thingy for us.  There really is no point
 * to having an additional scope parameter.
 * </p><p>
 * Perhaps we can also inject a new revisions (multi-valued) operational attribute into 
 * entries to track the revisions of changes in the changeLog to that entry.  This can
 * be used to ask the server for a log of changes that have been performed on a specific 
 * entry.  Whoa that's really hot for auditing!
 * </p><p>
 * We could try to do the same thing (meaning having a tags operational attribute) with revisions.
 * However this is pointless since the tag revision would already be in the revisions attribute.  Also
 * a tag would select entries dynamically: all entries with revisions below the tag revision would be
 * selected in the tag.  This leads to a neat idea: you can easily regenerate not only the revision 
 * history of an entry, you can do it for an entire subtree, and furthermore you might even be able
 * to conduct search operations based on a tag and the state of the server in the past.  This would be
 * pretty wild.
 * </p><p>
 * Another neat thing that could be done is to request an attribute by revision using the protocol 
 * based tagging mechanism in LDAP.  For example we have language based tags like cn;lang-en so why
 * not have version based tags like cn;revision-23.  When requested in this mannar the server can 
 * reconstruct the state of the attribute at a specific revision and return it to the user.  This is
 * an incredible capability when storing the configurations of systems in LDAP.  Being able to rollback
 * to a previous configuration or just inquire about a previous state is a powerful feature to have.
 * </p>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

package org.apache.directory.server.core.changelog;


