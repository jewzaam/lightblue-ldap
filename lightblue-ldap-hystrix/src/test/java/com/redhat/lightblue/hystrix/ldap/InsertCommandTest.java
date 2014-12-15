package com.redhat.lightblue.hystrix.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;

import org.junit.Rule;
import org.junit.Test;

import com.redhat.lightblue.ldap.test.LdapServerExternalResource;
import com.redhat.lightblue.ldap.test.LdapServerExternalResource.InMemoryLdapServer;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;

@InMemoryLdapServer
public class InsertCommandTest {

    @SuppressWarnings("serial")
    @Rule
    public LdapServerExternalResource ldapServer = new LdapServerExternalResource(new LinkedHashMap<String, Attribute[]>(){{
        put("dc=com", new Attribute[]{
                new Attribute("objectClass", "top"),
                new Attribute("objectClass", "domain"),
                new Attribute("dc", "com")});
        put("dc=example,dc=com", new Attribute[]{
                new Attribute("objectClass", "top"),
                new Attribute("objectClass", "domain"),
                new Attribute("dc", "example")});
    }});

    @Test
    public void testExecute() throws LDAPException{
        LDAPConnection connection = new LDAPConnection("localhost", LdapServerExternalResource.DEFAULT_PORT);

        Entry entry = new Entry("uid=john.doe,dc=example,dc=com",
                new Attribute("objectClass", "top", "person", "organizationalPerson", "inetOrgPerson"),
                new Attribute("uid", "john.doe"),
                new Attribute("givenName", "John"),
                new Attribute("sn", "Doe"),
                new Attribute("cn", "John Doe")
                );

        InsertCommand command = new InsertCommand(connection, entry);

        LDAPResult result = command.execute();

        assertNotNull(result);
        assertEquals(ResultCode.SUCCESS, result.getResultCode());
    }

}