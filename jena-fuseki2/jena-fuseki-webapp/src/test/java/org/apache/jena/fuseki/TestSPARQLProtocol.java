/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.fuseki;

import static org.apache.jena.fuseki.ServerCtl.serviceGSP;
import static org.apache.jena.fuseki.ServerCtl.serviceQuery;
import static org.apache.jena.fuseki.ServerCtl.serviceUpdate;
import static org.apache.jena.fuseki.ServerTest.*;
import static org.junit.Assert.assertTrue;

import org.apache.jena.query.*;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.exec.http.GSP;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.util.Convert;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.junit.Before;
import org.junit.Test;

public class TestSPARQLProtocol extends AbstractFusekiTest
{
    @Before
    public void before() {
        GSP.request(serviceGSP()).defaultGraph().PUT(graph1);
        GSP.request(serviceGSP()).graphName(gn1).PUT(graph2);
    }

    static String query(String base, String queryString) {
        return base + "?query=" + Convert.encWWWForm(queryString);
    }

    @Test
    public void query_01() {
        Query query = QueryFactory.create("SELECT * { ?s ?p ?o }");
        QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceQuery(), query);
        ResultSet rs = qexec.execSelect();
        int x = ResultSetFormatter.consume(rs);
        assertTrue(x != 0);
    }

    @Test
    public void query_02() {
        Query query = QueryFactory.create("SELECT * { ?s ?p ?o }");
        QueryExecution qExec = QueryExecutionHTTP.create()
                .service(serviceQuery())
                .query(query)
                .acceptHeader(WebContent.contentTypeResultsJSON)
                .build();
        ResultSet rs = qExec.execSelect();
        int x = ResultSetFormatter.consume(rs);
        assertTrue(x != 0);
    }

    @Test
    public void update_01() {
        UpdateRequest update = UpdateFactory.create("INSERT DATA {}");
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(update, serviceUpdate());
        proc.execute();
    }

    @Test
    public void update_02() {
        UpdateRequest update = UpdateFactory.create("INSERT DATA {}");
        UpdateProcessor proc = UpdateExecutionFactory.createRemoteForm(update, serviceUpdate());
        proc.execute();
    }
}
