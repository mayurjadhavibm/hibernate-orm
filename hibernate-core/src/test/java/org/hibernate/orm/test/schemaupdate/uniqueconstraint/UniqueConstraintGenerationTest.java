/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class UniqueConstraintGenerationTest {
	private File output;
	private MetadataImplementor metadata;
	StandardServiceRegistry ssr;

	@Before
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addResource( "org/hibernate/orm/test/schemaupdate/uniqueconstraint/TestEntity.hbm.xml" )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11101")
	public void testUniqueConstraintIsGenerated() throws Exception {
		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		if ( !(getDialect().getUniqueDelegate() instanceof SkipNullableUniqueDelegate) ) {
			if ( getDialect().getUniqueDelegate() instanceof AlterTableUniqueIndexDelegate ) {
				assertThat(
						"The test_entity_item table unique constraint has not been generated",
						isCreateUniqueIndexGenerated("test_entity_item", "item"),
						is(true)
				);
			}
			else {
				assertThat(
						"The test_entity_item table unique constraint has not been generated",
						isUniqueConstraintGenerated("test_entity_item", "item"),
						is(true)
				);
			}

			assertThat(
					"The test_entity_children table unique constraint has not been generated",
					isUniqueConstraintGenerated( "test_entity_children", "child" ),
					is( true )
			);
		}
	}

	private Dialect getDialect() {
		return ssr.getService(JdbcEnvironment.class).getDialect();
	}

	private boolean isUniqueConstraintGenerated(String tableName, String columnName) throws IOException {
		final String regex;
		Dialect dialect = getDialect();
		if ( dialect.getUniqueDelegate() instanceof CreateTableUniqueDelegate ) {
			regex = dialect.getCreateTableString() + " " + tableName + " .* " + columnName + " .+ unique.*\\)"
					+ dialect.getTableTypeString().toLowerCase() + ";";
		}
		else if ( dialect.getUniqueDelegate() instanceof AlterTableUniqueDelegate) {
			regex = dialect.getAlterTableString( tableName ) + " add constraint uk.* unique \\(" + columnName + "\\);";
		}
		else {
			return true;
		}

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		for ( String line : split ) {
			if ( line.matches(regex) ) {
				return true;
			}
		}
		return false;
	}

	private boolean isCreateUniqueIndexGenerated(String tableName, String columnName) throws IOException {
		String regex = "create unique (nonclustered )?index uk.* on " + tableName
				+ " \\(" + columnName + "\\)( where .*| exclude null keys)?;";
		final String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		Pattern p = Pattern.compile( regex );
		for ( String line : split ) {
			final Matcher matcher = p.matcher( line );
			if ( matcher.matches() ) {
				return true;
			}
		}
		return false;
	}
}
