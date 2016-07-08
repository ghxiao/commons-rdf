/**
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

package org.apache.commons.rdf.jena;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.QuadLike;
import org.apache.commons.rdf.api.RDFParserBuilder;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.RDFTermFactory;
import org.apache.commons.rdf.api.TripleLike;
import org.apache.commons.rdf.simple.AbstractRDFParserBuilder;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;

public class JenaRDFParserBuilder extends AbstractRDFParserBuilder<JenaRDFParserBuilder> implements RDFParserBuilder {

	private Consumer<TripleLike<RDFTerm, RDFTerm, RDFTerm>> generalizedConsumerTriple;
	private Consumer<QuadLike<RDFTerm, RDFTerm, RDFTerm, RDFTerm>> generalizedConsumerQuad;

	protected RDFTermFactory createRDFTermFactory() {
		return new JenaRDFTermFactory();
	}

	public JenaRDFParserBuilder targetGeneralizedTriple(Consumer<TripleLike<RDFTerm,RDFTerm,RDFTerm>> consumer) {
		JenaRDFParserBuilder c = this.clone();
		c.resetTarget();		
		c.generalizedConsumerTriple = consumer;
		return c;
	}

	public JenaRDFParserBuilder targetGeneralizedQuad(Consumer<QuadLike<RDFTerm,RDFTerm,RDFTerm,RDFTerm>> consumer) {
		JenaRDFParserBuilder c = this.clone();
		c.resetTarget();		
		c.generalizedConsumerQuad = consumer;
		return c;
	}
	
	@Override
	protected void resetTarget() {		
		super.resetTarget();
		this.generalizedConsumerTriple = null;
		this.generalizedConsumerQuad = null;
	}
	
	@Override
	protected void parseSynchronusly() throws IOException {
		StreamRDF dest;
		if (getTargetGraph().isPresent() && getTargetGraph().get() instanceof JenaGraph) {
			Graph jenaGraph = ((JenaGraph) getTargetGraph().get()).asJenaGraph();
			dest = StreamRDFLib.graph(jenaGraph);
		} else if (generalizedConsumerQuad != null) {				
			dest = getJenaFactory().streamJenaToGeneralizedQuad(generalizedConsumerQuad);			
		} else if (generalizedConsumerTriple != null) {				
			dest = getJenaFactory().streamJenaToGeneralizedTriple(generalizedConsumerTriple);			
		} else {
			dest = JenaRDFTermFactory.streamJenaToCommonsRDF(getRdfTermFactory().get(), getTarget());
		}

		Lang lang = getContentTypeSyntax().flatMap(JenaRDFTermFactory::rdfSyntaxToLang).orElse(null);
		String baseStr = getBase().map(IRI::getIRIString).orElse(null);

		if (getSourceIri().isPresent()) {
			RDFDataMgr.parse(dest, getSourceIri().get().toString(), baseStr, lang, null);
		} else if (getSourceFile().isPresent()) {
			try (InputStream s = Files.newInputStream(getSourceFile().get())) {
				RDFDataMgr.parse(dest, s, baseStr, lang, null);
			}
		} else {
			RDFDataMgr.parse(dest, getSourceInputStream().get(), baseStr, lang, null);
		}
	}

	private JenaRDFTermFactory getJenaFactory() {
		return (JenaRDFTermFactory) getRdfTermFactory()
				.filter(JenaRDFTermFactory.class::isInstance)
				.orElseGet(this::createRDFTermFactory);		
	}

}
