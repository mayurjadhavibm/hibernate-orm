/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import jakarta.persistence.spi.PersistenceUnitInfo;

import static java.util.Collections.emptyMap;
import static org.hibernate.jpa.boot.spi.Bootstrap.getEntityManagerFactoryBuilder;

public class JpaStaticMetamodelGenerator {

	public static void processMetamodel(PersistenceUnitInfo persistenceUnitInfo, GenerationOptions options) {
		final EntityManagerFactoryBuilder target = getEntityManagerFactoryBuilder( persistenceUnitInfo, emptyMap() );
		try {
			new JpaStaticMetamodelGenerator( options, target.metadata() ).process();
		}
		finally {
			target.cancel();
		}
	}

	private final GenerationOptions options;
	private final MetadataImplementor metadata;

	private final Directory generationOutputDirectory;
	private final ObjectFactory objectFactory;

	private final Set<String> processedDomainTypeNames = new HashSet<>();

	private JpaStaticMetamodelGenerator(GenerationOptions options, MetadataImplementor metadata) {
		this.options = options;
		this.metadata = metadata;
		this.generationOutputDirectory = options.getGenerationDirectory().get();
		this.objectFactory = new ObjectFactory( metadata );
	}

	private void process() {
		final Set<MappedSuperclass> mappedSuperclasses = metadata.getMappedSuperclassMappingsCopy();
		if ( mappedSuperclasses != null ) {
			mappedSuperclasses.forEach( this::handleMappedClass );
		}

		final Collection<PersistentClass> entityBindings = metadata.getEntityBindings();
		if ( entityBindings != null ) {
			entityBindings.forEach( this::handlePersistentClass );
		}
	}

	private void handleMappedClass(MappedSuperclass mappingDescriptor) {
		final MetamodelClass metamodelClass = objectFactory.metamodelClass( mappingDescriptor );
		handleManagedClass( metamodelClass, mappingDescriptor.getDeclaredProperties() );
	}

	private void handlePersistentClass(PersistentClass persistentClass) {
		final MetamodelClass metamodelClass = objectFactory.metamodelClass( persistentClass );
		handleManagedClass( metamodelClass, persistentClass.getDeclaredProperties() );
	}

	private void handleManagedClass(MetamodelClass metamodelClass, List<Property> properties) {
		if ( processedDomainTypeNames.add( metamodelClass.getDomainClassName() ) ) {
			// not yet processed
			for ( Property property : properties ) {
				metamodelClass.addAttribute(
						objectFactory.attribute( property, property.getValue(), metamodelClass, this::handleEmbeddable )
				);
			}

			final String replaced = metamodelClass.getMetamodelClassName().replace( '.', '/' );
			final String metamodelClassJavaFileName = replaced + ".java";
			final RegularFile metamodelClassJavaFile = generationOutputDirectory.file( metamodelClassJavaFileName );

			final File metamodelClassJavaFileAsFile = metamodelClassJavaFile.getAsFile();
			metamodelClass.writeToFile( metamodelClassJavaFileAsFile, options );
		}
	}

	private void handleEmbeddable(Component embeddedValueMapping) {
		final MetamodelClass metamodelClass = objectFactory.metamodelClass( embeddedValueMapping );
		handleManagedClass( metamodelClass, embeddedValueMapping.getProperties() );
	}
}
