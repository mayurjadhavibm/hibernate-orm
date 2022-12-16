/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping;

import java.time.Instant;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.CardPayment;
import org.hibernate.testing.orm.domain.retail.CashPayment;
import org.hibernate.testing.orm.domain.retail.DomesticVendor;
import org.hibernate.testing.orm.domain.retail.ForeignVendor;
import org.hibernate.testing.orm.domain.retail.Payment;
import org.hibernate.testing.orm.domain.retail.Product;
import org.hibernate.testing.orm.domain.retail.Vendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests access to {@link MappingMetamodel} as API and SPI contracts
 *
 * @author Steve Ebersole
 */
@DomainModel(standardModels = StandardDomainModel.RETAIL)
@SessionFactory(exportSchema = false)
public class MappingModelAccessTests {
	@Test
	public void testUnwrapAccess(SessionFactoryScope scope) {
		final SessionFactoryImplementor sf = scope.getSessionFactory();

		final MappingMetamodel mappingMetamodel = sf.unwrap( MappingMetamodel.class );
		assertThat( mappingMetamodel ).isNotNull();

		final MappingMetamodelImplementor mappingMetamodelImplementor = sf.unwrap( MappingMetamodelImplementor.class );
		assertThat( mappingMetamodelImplementor ).isSameAs( mappingMetamodel );
	}

	@Test
	void testEntityMappingAccess(SessionFactoryScope scope) {
		final SessionFactoryImplementor sf = scope.getSessionFactory();
		final MappingMetamodel mappingMetamodel = sf.unwrap( MappingMetamodel.class );

		final EntityMappingType productMapping = mappingMetamodel.getEntityDescriptor( Product.class );
		assertThat( productMapping ).isNotNull();

		final EntityIdentifierMapping productIdMapping = productMapping.getIdentifierMapping();
		assertThat( productIdMapping ).isNotNull();
		assertThat( productIdMapping.getJavaType().getJavaTypeClass() ).isEqualTo( Integer.class );

		final EntityVersionMapping productVersionMapping = productMapping.getVersionMapping();
		assertThat( productVersionMapping ).isNotNull();
		assertThat( productVersionMapping.getVersionAttribute().getAttributeName() ).isEqualTo( "version" );
		assertThat( productVersionMapping.getVersionAttribute().getJavaType().getJavaTypeClass() ).isEqualTo( Instant.class );
		assertThat( productVersionMapping.asAttributeMapping() ).isSameAs( productVersionMapping.getVersionAttribute() );
	}

	@Test
	void testJoinedSubclassInheritance(SessionFactoryScope scope) {
		final SessionFactoryImplementor sf = scope.getSessionFactory();
		final MappingMetamodel mappingMetamodel = sf.unwrap( MappingMetamodel.class );

		final EntityMappingType paymentMapping = mappingMetamodel.getEntityDescriptor( Payment.class );
		final EntityMappingType cardPaymentMapping = mappingMetamodel.getEntityDescriptor( CardPayment.class );
		final EntityMappingType cashPaymentMapping = mappingMetamodel.getEntityDescriptor( CashPayment.class );

		final EntityDiscriminatorMapping discriminatorMapping = paymentMapping.getDiscriminatorMapping();
		assertThat( discriminatorMapping )
				.isSameAs( cardPaymentMapping.getDiscriminatorMapping() )
				.isSameAs( cashPaymentMapping.getDiscriminatorMapping() );

		assertThat( discriminatorMapping.isPhysical() ).isFalse();
		assertThat( discriminatorMapping.isVirtual() ).isTrue();
		assertThat( discriminatorMapping.getJavaType().getJavaTypeClass() ).isEqualTo( Class.class );
		assertThat( discriminatorMapping.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Class.class );
		assertThat( discriminatorMapping.getJdbcMapping().getJdbcJavaType().getJavaTypeClass() ).isEqualTo( Integer.class );

		assertThat( paymentMapping.getDiscriminatorValue() ).isEqualTo( 0 );
		assertThat( cashPaymentMapping.getDiscriminatorValue() ).isEqualTo( 1 );
		assertThat( cardPaymentMapping.getDiscriminatorValue() ).isEqualTo( 2 );

		assertThat( discriminatorMapping.resolveDiscriminatorValue( 0 ).getIndicatedEntity() ).isEqualTo( paymentMapping );
		assertThat( discriminatorMapping.resolveDiscriminatorValue( 1 ).getIndicatedEntity() ).isEqualTo( cashPaymentMapping );
		assertThat( discriminatorMapping.resolveDiscriminatorValue( 2 ).getIndicatedEntity() ).isEqualTo( cardPaymentMapping );
		assertThat( discriminatorMapping.resolveDiscriminatorValue( 3 ) ).isNull();
	}

	@Test
	void testSingleTableInheritance(SessionFactoryScope scope) {
		final SessionFactoryImplementor sf = scope.getSessionFactory();
		final MappingMetamodel mappingMetamodel = sf.unwrap( MappingMetamodel.class );

		final EntityMappingType vendorMapping = mappingMetamodel.getEntityDescriptor( Vendor.class );
		final EntityMappingType domesticVendorMapping = mappingMetamodel.getEntityDescriptor( DomesticVendor.class );
		final EntityMappingType foreignVendorMapping = mappingMetamodel.getEntityDescriptor( ForeignVendor.class );

		final EntityDiscriminatorMapping discriminatorMapping = vendorMapping.getDiscriminatorMapping();
		assertThat( discriminatorMapping )
				.isSameAs( domesticVendorMapping.getDiscriminatorMapping() )
				.isSameAs( foreignVendorMapping.getDiscriminatorMapping() );

		assertThat( discriminatorMapping.isPhysical() ).isTrue();
		assertThat( discriminatorMapping.isVirtual() ).isTrue();
		assertThat( discriminatorMapping.getJavaType().getJavaTypeClass() ).isEqualTo( Class.class );
		assertThat( discriminatorMapping.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( Class.class );
		assertThat( discriminatorMapping.getJdbcMapping().getJdbcJavaType().getJavaTypeClass() ).isEqualTo( String.class );

		assertThat( vendorMapping.getDiscriminatorValue() ).isEqualTo( "Vendor" );
		assertThat( domesticVendorMapping.getDiscriminatorValue() ).isEqualTo( "domestic" );
		assertThat( foreignVendorMapping.getDiscriminatorValue() ).isEqualTo( "foreign" );

		assertThat( discriminatorMapping.resolveDiscriminatorValue( "Vendor" ).getIndicatedEntity() ).isEqualTo( vendorMapping );
		assertThat( discriminatorMapping.resolveDiscriminatorValue( "domestic" ).getIndicatedEntity() ).isEqualTo( domesticVendorMapping );
		assertThat( discriminatorMapping.resolveDiscriminatorValue( "foreign" ).getIndicatedEntity() ).isEqualTo( foreignVendorMapping );
		assertThat( discriminatorMapping.resolveDiscriminatorValue( "invalid" ) ).isNull();
	}
}
