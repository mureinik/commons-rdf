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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.QuadLike;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.RDFTermFactory;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.api.TripleLike;
import org.apache.commons.rdf.jena.impl.JenaFactory;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * RDFTermFactory with Jena-backed objects.
 * <p>
 * This factory can also convert existing objects from/to Jena with methods like
 * {@link #fromJena(org.apache.jena.graph.Graph)} and {@link #toJena(Graph)}.
 * 
 * @see RDFTermFactory
 */
public final class JenaRDFTermFactory implements RDFTermFactory {

	private UUID salt;

	public JenaRDFTermFactory() {
		this.salt = UUID.randomUUID();
	}

	public JenaRDFTermFactory(UUID salt) {
		this.salt = salt;
	}

	@Override
	public JenaBlankNode createBlankNode() {
		return JenaFactory.createBlankNode(salt);
	}

	@Override
	public JenaBlankNode createBlankNode(String name) {
		return JenaFactory.createBlankNode(name, salt);
	}

	@Override
	public JenaGraph createGraph() {
		return JenaFactory.createGraph(salt);
	}

	@Override
	public JenaIRI createIRI(String iri) {
		validateIRI(iri);
		return JenaFactory.createIRI(iri);
	}

	@Override
	public JenaLiteral createLiteral(String lexicalForm) {
		return JenaFactory.createLiteral(lexicalForm);
	}

	@Override
	public JenaLiteral createLiteral(String lexicalForm, IRI dataType) {
		return JenaFactory.createLiteralDT(lexicalForm, dataType.getIRIString());
	}

	@Override
	public JenaLiteral createLiteral(String lexicalForm, String languageTag) {
		validateLang(languageTag);
		return JenaFactory.createLiteralLang(lexicalForm, languageTag);
	}

	@Override
	public JenaTriple createTriple(BlankNodeOrIRI subject, IRI predicate, RDFTerm object) {
		return JenaFactory.createTriple(subject, predicate, object);
	}
	
	@Override
	public Quad createQuad(BlankNodeOrIRI graphName, BlankNodeOrIRI subject, IRI predicate, RDFTerm object)
			throws IllegalArgumentException, UnsupportedOperationException {
		return JenaFactory.createQuad(subject, predicate, object, graphName);
	}
	
	/**
	 * Adapt a generalized Jena Triple to a CommonsRDF {@link TripleLike} statement.
	 * <p>
	 * The generalized triple supports any {@link RDFTerm} as its {@link TripleLike#getSubject()}
	 * {@link TripleLike#getPredicate()} or {@link TripleLike#getObject()}. 
	 * <p>
	 * If the Jena triple contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use a {@link UUID} salt from this
	 * {@link JenaRDFTermFactory} instance in combination with
	 * {@link Node#getBlankNodeId()} for the purpose of its
	 * {@link BlankNode#uniqueReference()}.
	 *
	 * @see #fromJena(org.apache.jena.graph.Triple, UUID)
	 * @see #fromJena(RDFTermFactory, org.apache.jena.graph.Triple)
	 * 
	 * @param subject The subject of the statement
	 *            
	 * @return Adapted {@link TripleLike}. Note that the generalized triple does
	 *         <strong>not</strong> implement {@link Triple#equals(Object)} or
	 *         {@link Triple#hashCode()}.
	 * @throws ConversionException
	 *             if any of the triple's nodes are not concrete
	 */
	public JenaTripleLike<RDFTerm, RDFTerm, RDFTerm> createGeneralizedTriple(
			RDFTerm subject, RDFTerm predicate, RDFTerm object) {
		return JenaFactory.createGeneralizedTriple(subject, predicate, object);
	}

	/**
	 * Adapt an existing Jena Node to CommonsRDF {@link RDFTerm}.
	 * <p>
	 * If {@link Node#isLiteral()}, then the returned value is a {@link Literal}.
	 * If {@link Node#isURI(), the returned value is a IRI. If Node#isBlank(),
	 * the returned value is a {@link BlankNode}, which will use a {@link UUID}
	 * salt from this {@link JenaRDFTermFactory} instance in combination with
	 * {@link Node#getBlankNodeId()} for the purpose of its
	 * {@link BlankNode#uniqueReference()}.
	 * 
	 * @see #fromJena(Node, UUID)
	 * @see #fromJena(RDFTermFactory, Node)
	 * 
	 * @param node
	 *            The Jena Node to adapt. It's {@link Node#isConcrete()} must be
	 *            <code>true</code>.
	 * @throws ConversionException
	 *             if the node is not concrete.
	 */
	public JenaRDFTerm fromJena(Node node) throws ConversionException {
		return JenaFactory.fromJena(node, salt);
	}

	/**
	 * Adapt an existing Jena Node to CommonsRDF {@link RDFTerm}.
	 * <p>
	 * If {@link Node#isLiteral()}, then the returned value is a {@link Literal}
	 * . If {@link Node#isURI(), the returned value is a IRI. If Node#isBlank(),
	 * the returned value is a {@link BlankNode}, which will use the provided
	 * {@link UUID} salt in combination with {@link Node#getBlankNodeId()} for
	 * the purpose of its {@link BlankNode#uniqueReference()}.
	 * 
	 * @see #fromJena(Node)
	 * @see #fromJena(RDFTermFactory, Node)
	 * 
	 * @param node
	 *            The Jena Node to adapt. It's {@link Node#isConcrete()} must be
	 *            <code>true</code>.
	 * @param salt
	 *            UUID salt for the purpose of
	 *            {@link BlankNode#uniqueReference()}
	 * @throws ConversionException
	 *             if the node is not concrete.
	 */
	public static JenaRDFTerm fromJena(Node node, UUID salt) {
		return JenaFactory.fromJena(node, salt);
	}
	
	/**
	 * Convert from Jena {@link Node} to any RDFCommons implementation.
	 * <p>
	 * Note that if the {@link Node#isBlank()}, then the factory's 
	 * {@link RDFTermFactory#createBlankNode(String)} will be used, meaning
	 * that care should be taken if reusing an {@link RDFTermFactory} instance
	 * for multiple conversion sessions.
	 * 
	 * @see #fromJena(Node)
	 * @see #fromJena(Node, UUID)
	 * 
	 * @param factory {@link RDFTermFactory} to use for creating {@link RDFTerm}.
	 * @param node
	 *            The Jena Node to adapt. It's {@link Node#isConcrete()} must be
	 *            <code>true</code>.
	 * @throws ConversionException
	 *             if the node is not concrete.
	 */
	public static RDFTerm fromJena(RDFTermFactory factory, Node node) {
		if (node == null) {
			return null;
		}
		if (factory instanceof JenaRDFTermFactory) {
			// No need to convert, just wrap
			return ((JenaRDFTermFactory) factory).fromJena(node);
		}
		if (node.isURI())
			return factory.createIRI(node.getURI());
		if (node.isLiteral()) {
			String lang = node.getLiteralLanguage();
			if (lang != null && !lang.isEmpty())
				return factory.createLiteral(node.getLiteralLexicalForm(), lang);
			if (node.getLiteralDatatype().equals(XSDDatatype.XSDstring))
				return factory.createLiteral(node.getLiteralLexicalForm());
			IRI dt = factory.createIRI(node.getLiteralDatatype().getURI());
			return factory.createLiteral(node.getLiteralLexicalForm(), dt);
		}
		if (node.isBlank())
			// The factory
			return factory.createBlankNode(node.getBlankNodeLabel());
		throw new ConversionException("Node is not a concrete RDF Term: " + node);
	}	
	
	/**
	 * Adapt an existing Jena Triple to CommonsRDF {@link Triple}.
	 * <p>
	 * If the triple contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use a {@link UUID} salt from this
	 * {@link JenaRDFTermFactory} instance in combination with
	 * {@link Node#getBlankNodeId()} for the purpose of its
	 * {@link BlankNode#uniqueReference()}.
	 *
	 * @see #fromJena(org.apache.jena.graph.Triple, UUID)
	 * @see #fromJena(RDFTermFactory, org.apache.jena.graph.Triple)
	 * 
	 * @param triple
	 *            Jena triple
	 * @return Adapted triple
	 * @throws ConversionException
	 *             if any of the triple's nodes are not concrete or the triple
	 *             is a generalized triple
	 */
	public JenaTriple fromJena(org.apache.jena.graph.Triple triple) throws ConversionException {
		return JenaFactory.fromJena(triple, salt);
	}


	/**
	 * Adapt a generalized Jena Triple to a CommonsRDF {@link TripleLike}.
	 * <p>
	 * The generalized triple supports any {@link RDFTerm} as its {@link TripleLike#getSubject()}
	 * {@link TripleLike#getPredicate()} or {@link TripleLike#getObject()}. 
	 * <p>
	 * If the Jena triple contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use the provided {@link UUID} salt 
	 * in combination with
	 * {@link Node#getBlankNodeId()} for the purpose of its
	 * {@link BlankNode#uniqueReference()}.
	 *
	 * @see #fromJena(org.apache.jena.graph.Triple, UUID)
	 * @see #fromJena(RDFTermFactory, org.apache.jena.graph.Triple)
	 * 
	 * @param triple
	 *            Jena triple
	 * @param salt
	 *            UUID salt for the purpose of
	 *            {@link BlankNode#uniqueReference()}
	 * @return Adapted {@link TripleLike}. Note that the generalized triple does
	 *         <strong>not</strong> implement {@link Triple#equals(Object)} or
	 *         {@link Triple#hashCode()}.
	 * @throws ConversionException
	 *             if any of the triple's nodes are not concrete
	 */
	public JenaTripleLike<RDFTerm, RDFTerm, RDFTerm> fromJenaGeneralized(org.apache.jena.graph.Triple triple, UUID salt) throws ConversionException {
		return JenaFactory.fromJenaGeneralized(triple, salt);
	}
	
	/**
	 * Adapt a generalized Jena {@link org.apache.jena.graph.Triple} to a CommonsRDF {@link TripleLike}.
	 * <p>
	 * The generalized triple supports any {@link RDFTerm} as its {@link TripleLike#getSubject()}
	 * {@link TripleLike#getPredicate()} or {@link TripleLike#getObject()}, including 
	 * the extensions {@link JenaAny} and {@link JenaVariable}.
	 * <p>
	 * If the Jena triple contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use a {@link UUID} salt from this
	 * {@link JenaRDFTermFactory} instance in combination with
	 * {@link Node#getBlankNodeId()} for the purpose of its
	 * {@link BlankNode#uniqueReference()}.
	 *
	 * @see #fromJena(org.apache.jena.graph.Triple, UUID)
	 * @see #fromJena(RDFTermFactory, org.apache.jena.graph.Triple)
	 * 
	 * @param triple
	 *            Jena triple
	 * @return Adapted {@link TripleLike}. Note that the generalized triple does
	 *         <strong>not</strong> implement {@link Triple#equals(Object)} or
	 *         {@link Triple#hashCode()}.
	 * @throws ConversionException
	 *             if any of the triple's nodes are not concrete
	 */
	public JenaTripleLike<RDFTerm, RDFTerm, RDFTerm> fromJenaGeneralized(org.apache.jena.graph.Triple triple) throws ConversionException {
		return JenaFactory.fromJenaGeneralized(triple, salt);
	}

	/**
	 * Adapt a generalized Jena {@link org.apache.jena.sparql.core.Quad} to a CommonsRDF {@link QuadLike}.
	 * <p>
	 * The generalized quad supports any {@link RDFTerm} as its 
	 * {@link QuadLike#getGraphName()}, 
	 * {@link QuadLike#getSubject()}
	 * {@link QuadLike#getPredicate()} or 
	 * {@link QuadLike#getObject()}, including 
	 * the extensions 
	 * {@link JenaAny} and {@link JenaVariable}. 
	 * <p>
	 * If the Jena quad contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use a {@link UUID} salt from this
	 * {@link JenaRDFTermFactory} instance in combination with
	 * {@link Node#getBlankNodeId()} for the purpose of its
	 * {@link BlankNode#uniqueReference()}.
	 *
	 * @see #fromJena(org.apache.jena.graph.Quad, UUID)
	 * @see #fromJena(RDFTermFactory, org.apache.jena.graph.Quad)
	 * 
	 * @param quad
	 *            Jena quad
	 * @return Adapted {@link QuadLike}. Note that the generalized quad does
	 *         <strong>not</strong> implement {@link Quad#equals(Object)} or
	 *         {@link Quad#hashCode()}.
	 * @throws ConversionException
	 *             if any of the quad nodes are not concrete
	 */
	public JenaQuadLike<RDFTerm, RDFTerm, RDFTerm, RDFTerm> fromJenaGeneralized(org.apache.jena.sparql.core.Quad quad) throws ConversionException {
		return JenaFactory.fromJenaGeneralized(quad, salt);
	}
	
	
	/**
	 * Adapt an existing Jena Triple to CommonsRDF {@link Triple}.
	 * <p>
	 * If the triple contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use the provided a {@link UUID} salt in
	 * combination with {@link Node#getBlankNodeId()} for the purpose of its
	 * {@link BlankNode#uniqueReference()}.
	 * 
	 * @param triple
	 *            Jena triple
	 * @param salt
	 *            A {@link UUID} salt for adapting any {@link BlankNode}s
	 * @return Adapted triple
	 * @throws ConversionException
	 *             if any of the triple's nodes are not concrete or the triple
	 *             is a generalized triple
	 */
	public static JenaTriple fromJena(org.apache.jena.graph.Triple triple, UUID salt) throws ConversionException {
		return JenaFactory.fromJena(triple, salt);
	}

	/**
	 * Convert from Jena {@link org.apache.jena.graph.Triple} to any RDFCommons
	 * implementation.
	 * <p>
	 * Note that if any of the triple's nodes {@link Node#isBlank()}, then the factory's 
	 * {@link RDFTermFactory#createBlankNode(String)} will be used, meaning
	 * that care should be taken if reusing an {@link RDFTermFactory} instance
	 * for multiple conversion sessions.
	 * 
	 * @see #fromJena(org.apache.jena.graph.Triple)
	 * @see #fromJena(org.apache.jena.graph.Triple, UUID)
	 *
	 * @param factory {@link RDFTermFactory} to use for creating the {@link Triple} and its
	 * {@link RDFTerm}s.
	 * @param triple
	 *            Jena triple
	 * @return Converted triple
	 * @throws ConversionException
	 *             if any of the triple's nodes are not concrete or the triple
	 *             is a generalized triple
	 */
	public static Triple fromJena(RDFTermFactory factory, org.apache.jena.graph.Triple triple) 
			throws ConversionException{
		if (factory instanceof JenaRDFTermFactory) {
			// No need to convert, just wrap
			return ((JenaRDFTermFactory) factory).fromJena(triple);
		}
		BlankNodeOrIRI subject;
		IRI predicate;
		try {
			subject = (BlankNodeOrIRI) fromJena(factory, triple.getSubject());
			predicate = (IRI) fromJena(factory, triple.getPredicate());
		} catch (ClassCastException ex) {
			throw new ConversionException("Can't convert generalized triple: " + triple, ex);
		}
		RDFTerm object = fromJena(factory, triple.getObject());
		return factory.createTriple(subject, predicate, object);
	}

	/**
	 * Adapt an existing Jena Quad to CommonsRDF {@link Quad}.
	 * <p>
	 * If the quad contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use a {@link UUID} salt from this 
	 * {@link JenaRDFTermFactory} instance
	 * in combination with {@link Node#getBlankNodeId()} 
	 * for the purpose of its {@link BlankNode#uniqueReference()}.
	 * 
	 * @param quad
	 *            Jena quad
	 * @return Adapted quad
	 */	
	public Quad fromJena(org.apache.jena.sparql.core.Quad quad) {
		return JenaFactory.fromJena(quad, salt);
	}
	
	/**
	 * Adapt an existing Jena Quad to CommonsRDF {@link Quad}.
	 * <p>
	 * If the quad contains any {@link Node#isBlank()}, then any corresponding
	 * {@link BlankNode} will use the provided {@link UUID} salt
	 * in combination with {@link Node#getBlankNodeId()} 
	 * for the purpose of its {@link BlankNode#uniqueReference()}.
	 * 
	 * @param quad
	 *            Jena quad
	 * @param salt
	 *            A {@link UUID} salt for adapting any {@link BlankNode}s
	 * @return Adapted quad
	 */		
	public static Quad fromJena(org.apache.jena.sparql.core.Quad quad, UUID salt) {
		return JenaFactory.fromJena(quad, salt);
	}

	/**
	 * Adapt an existing Jena Graph to CommonsRDF {@link Graph}. This does not
	 * take a copy, changes to the CommonsRDF Graph are reflected in the jena
	 * graph.
	 */
	public static Graph fromJena(org.apache.jena.graph.Graph graph) {
		// NOTE: This generates a new UUID salt per graph
		return JenaFactory.fromJena(graph);
	}
	


	/**
	 * Convert from Jena to any RDFCommons implementation. This is a copy, even
	 * if the factory is a RDFTermFactoryJena. Use
	 * {@link #fromJena(org.apache.jena.graph.Graph)} for a wrapper.
	 */
	public static Graph fromJena(RDFTermFactory factory, org.apache.jena.graph.Graph graph) {
		if (factory instanceof JenaRDFTermFactory) {
			// No need to convert, just wrap
			return fromJena(graph);
		}

		Graph g = factory.createGraph();
		graph.find(Node.ANY, Node.ANY, Node.ANY).forEachRemaining(t -> {
			g.add(fromJena(factory, t));
		});
		return g;
	}


	public static Quad fromJena(RDFTermFactory factory, org.apache.jena.sparql.core.Quad quad) {
		if (factory instanceof JenaRDFTermFactory) {
			// No need to convert, just wrap
			return ((JenaRDFTermFactory) factory).fromJena(quad);
		}
		BlankNodeOrIRI graphName = (BlankNodeOrIRI) (fromJena(factory, quad.getGraph()));
		BlankNodeOrIRI subject = (BlankNodeOrIRI) (fromJena(factory, quad.getSubject()));
		IRI predicate = (IRI) (fromJena(factory, quad.getPredicate()));
		RDFTerm object = fromJena(factory, quad.getObject());
		return factory.createQuad(graphName, subject, predicate, object);
	}

	public static Optional<RDFSyntax> langToRdfSyntax(Lang lang) {
		return RDFSyntax.byMediaType(lang.getContentType().getContentType());
	}

	public static Optional<Lang> rdfSyntaxToLang(RDFSyntax rdfSyntax) {
		return Optional.ofNullable(RDFLanguages.contentTypeToLang(rdfSyntax.mediaType));
	}

	/**
	 * Create a {@link StreamRDF} that inserts into any RDFCommons
	 * implementation of Graph
	 */
	public static StreamRDF streamJenaToCommonsRDF(RDFTermFactory factory, Consumer<Quad> consumer) {
		return new StreamRDFBase() {
			@Override
			public void quad(org.apache.jena.sparql.core.Quad quad) {
				consumer.accept(fromJena(factory, quad));
			}
		};
	}
	
	/**
	 * Create a {@link StreamRDF} that inserts into any RDFCommons
	 * implementation of Graph
	 */
	public StreamRDF streamJenaToGeneralizedTriple(Consumer<TripleLike<RDFTerm, RDFTerm, RDFTerm>> generalizedConsumer) {
		return new StreamRDFBase() {			
			@Override
			public void triple(org.apache.jena.graph.Triple triple) {
				generalizedConsumer.accept(fromJenaGeneralized(triple));
			}
		};
	}	

	/**
	 * Create a {@link StreamRDF} that inserts into any RDFCommons
	 * implementation of Graph
	 */
	public StreamRDF streamJenaToGeneralizedQuad(Consumer<QuadLike<RDFTerm, RDFTerm, RDFTerm, RDFTerm>> generalizedConsumer) {
		return new StreamRDFBase() {
			@Override
			public void quad(org.apache.jena.sparql.core.Quad quad) {
				generalizedConsumer.accept(fromJenaGeneralized(quad));
			}
		};
	}	
	
	/**
	 * Convert a CommonsRDF Graph to a Jena Graph. If the Graph was from Jena
	 * originally, return that original object else create a copy using Jena
	 * objects.
	 */
	public static org.apache.jena.graph.Graph toJena(Graph graph) {
		if (graph instanceof JenaGraph)
			return ((JenaGraph) graph).asJenaGraph();
		org.apache.jena.graph.Graph g = GraphFactory.createGraphMem();
		graph.stream().forEach(t -> g.add(toJena(t)));
		return g;
	}

	/**
	 * Convert a CommonsRDF RDFTerm to a Jena Node. If the RDFTerm was from Jena
	 * originally, return that original object, else create a copy using Jena
	 * objects.
	 */
	public static Node toJena(RDFTerm term) {
		if (term == null) {
			return null;
		}
		if (term instanceof JenaRDFTerm)
			// TODO: What if it's a BlankNodeImpl with
			// a different salt? Do we need to rewrite the
			// jena blanknode identifier?
			return ((JenaRDFTerm) term).asJenaNode();

		if (term instanceof IRI)
			return NodeFactory.createURI(((IRI) term).getIRIString());

		if (term instanceof Literal) {
			Literal lit = (Literal) term;
			RDFDatatype dt = NodeFactory.getType(lit.getDatatype().getIRIString());
			String lang = lit.getLanguageTag().orElse("");
			return NodeFactory.createLiteral(lit.getLexicalForm(), lang, dt);
		}

		if (term instanceof BlankNode) {
			String id = ((BlankNode) term).uniqueReference();
			return NodeFactory.createBlankNode(id);
		}
		throw new ConversionException("Not a concrete RDF Term: " + term);
	}

	/**
	 * Convert a CommonsRDF Triple to a Jena Triple. If the Triple was from Jena
	 * originally, return that original object else create a copy using Jena
	 * objects.
	 */
	public static org.apache.jena.graph.Triple toJena(Triple triple) {
		if (triple instanceof JenaTriple)
			return ((JenaTriple) triple).asJenaTriple();
		return new org.apache.jena.graph.Triple(
				toJena(triple.getSubject()), 
				toJena(triple.getPredicate()),
				toJena(triple.getObject()));
	}

	// Some simple validations - full IRI parsing is not cheap.
	private static void validateIRI(String iri) {
		if (iri.contains(" "))
			throw new IllegalArgumentException();
		if (iri.contains("<"))
			throw new IllegalArgumentException();
		if (iri.contains(">"))
			throw new IllegalArgumentException();
	}

	private static void validateLang(String languageTag) {
		if (languageTag.contains(" "))
			throw new IllegalArgumentException("Invalid language tag: " + languageTag);
	}

}