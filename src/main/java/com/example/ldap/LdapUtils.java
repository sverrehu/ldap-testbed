package com.example.ldap;

import com.example.Utils;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public final class LdapUtils {

    private static final String DEFAULT_GROUP_MEMBER_ATTRIBUTE_NAME = "member";
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final Logger LOG = Logger.getLogger(LdapUtils.class.getName());
    private static volatile Properties nameProperties = null;

    private LdapUtils() {
    }

    public static Name getName(final String s) {
        if (nameProperties == null) {
            nameProperties = new Properties();
            nameProperties.setProperty("jndi.syntax.trimblanks", "true");
            nameProperties.setProperty("jndi.syntax.direction", "right_to_left");
            nameProperties.setProperty("jndi.syntax.ignorecase", "true");
            nameProperties.setProperty("jndi.syntax.separator", ",");
            nameProperties.setProperty("jndi.syntax.escape", "\\");
            nameProperties.setProperty("jndi.syntax.beginquote", "\"");
            nameProperties.setProperty("jndi.syntax.separator.ava", ",");
            nameProperties.setProperty("jndi.syntax.separator.typeval", "=");
        }
        try {
            return new CompoundName(s, nameProperties);
        } catch (final InvalidNameException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static LdapContext connect(final String url, final String userDn, final String password, final boolean usePooling) {
        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        if (usePooling) {
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.pool.timeout", "600000");
        }
        env.put(Context.PROVIDER_URL, url);
        if (!Utils.isBlank(userDn)) {
            if (Utils.isBlank(password)) {
                /* We need to stop this here, since some LDAP servers treat a blank password as an
                 * anonymous login, even if a userDn is provided. Stupid shit, particularly when the
                 * connect thing is the only way to perform LDAP authentication. */
                throw new IllegalArgumentException("Empty password not allowed.");
            }
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
        } else {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        try {
            return new InitialLdapContext(env, null);
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static void close(final LdapContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static NamingEnumeration<?> findByAttributeValue(final LdapContext ldap, final String attributeName, final String value, final String[] attributesToReturn) {
        final SearchControls sc = new SearchControls();
        if (attributesToReturn != null) {
            sc.setReturningAttributes(attributesToReturn);
        }
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ldap.search("", attributeName + "={0}", new String[] { value }, sc);
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static NamingEnumeration<?> findByAttributeValue(final LdapContext ldap, final String attributeName, final String value) {
        return findByAttributeValue(ldap, attributeName, value, null);
    }

    public static NamingEnumeration<?> findByUid(final LdapContext ldap, final String uid) {
        return findByAttributeValue(ldap, "uid", uid);
    }

    public static Attributes findByRdn(final LdapContext ldap, final String rdn, final String[] attributesToReturn) {
        try {
            return ldap.getAttributes(rdn, attributesToReturn);
        } catch (final NameNotFoundException e) {
            return null;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static Attributes findByRdn(final LdapContext ldap, final String rdn) {
        return findByRdn(ldap, rdn, null);
    }

    public static Set<String> findGroups(final LdapContext ldap, final String dn) {
        final Set<String> set = new HashSet<>();
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String escapedDn = LdapUtils.escape(dn);
        final String filter = "(|(&(uniqueMember=" + escapedDn + ")"
                              + "(objectClass=groupOfUniqueNames))"
                              + "(&(member=" + escapedDn
                              + ")(objectClass=groupOfNames)))";
        try {
            final NamingEnumeration<SearchResult> ne = ldap.search("", filter, sc);
            for (; ne.hasMore();) {
                final SearchResult sr = ne.next();
                final Attributes attributes = sr.getAttributes();
                if (attributes != null) {
                    final Attribute attribute = attributes.get("cn");
                    if (attribute != null) {
                        set.add(attribute.get().toString());
                    }
                }
            }
            return set;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    /* Both Dns must be fully qualified (not relative). */
    public static void addUserToGroup(final LdapContext ldap, final String userDn, final String groupDn, final String memberAttributeName) {
        final ModificationItem[] modItems = new ModificationItem[1];
        /* Using REPLACE_ATTRIBUTE rather than ADD_ATTRIBUTE, since the latter may complain if the
         * user is already a member. */
        modItems[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(memberAttributeName, userDn));
        try {
            ldap.modifyAttributes(groupDn, modItems);
        } catch (final AttributeInUseException e) {
            LOG.finest("Couldn't addUserToGroup.  User `" + userDn + "' was already a member of group `" + groupDn + "'");
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static void addUserToGroup(final LdapContext ldap, final String userDn, final String groupDn) {
        addUserToGroup(ldap, userDn, groupDn, DEFAULT_GROUP_MEMBER_ATTRIBUTE_NAME);
    }

    public static void removeUserFromGroup(final LdapContext ldap, final String userDn, final String groupDn, final String memberAttributeName) {
        final ModificationItem[] modItems = new ModificationItem[1];
        modItems[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(memberAttributeName, userDn));
        try {
            ldap.modifyAttributes(groupDn, modItems);
        } catch (final NoSuchAttributeException e) {
            LOG.finest("Couldn't removeUserFromGroup. User `" + userDn + "' wasn't a member of group `" + groupDn + "'");
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static void removeUserFromGroup(final LdapContext ldap, final String userDn, final String groupDn) {
        removeUserFromGroup(ldap, userDn, groupDn, DEFAULT_GROUP_MEMBER_ATTRIBUTE_NAME);
    }

    public static boolean isBelow(final String nodeName, final String treeName) {
        final Name node = getName(nodeName);
        final Name tree = getName(treeName);
        return node.startsWith(tree);
    }

    public static String escape(final String s) {
        /* See RFC 2253, section 2.4 */
        final StringBuilder sb = new StringBuilder();
        final int len = s.length();
        for (int q = 0; q < len; q++) {
            final int c = s.charAt(q);
            boolean doEscape = false;
            if (q == 0 && (c == ' ' || c == '#')) {
                doEscape = true;
            } else if (q == len - 1 && c == ' ') {
                doEscape = true;
            } else if (",+\"\\<>;".indexOf(c) >= 0) {
                doEscape = true;
            } else if (c < 32 || c > 126) {
                /* The standard actually allows values outside this range, but since we are allowed
                 * to escape anything, we do it just to avoid potential problems. */
                /* Update 2007-04-24: only escape the low ones. */
                if (c < 32) {
                    doEscape = true;
                }
            }
            if (doEscape) {
                sb.append('\\');
                if (" #,+\"\\<>;".indexOf(c) >= 0) {
                    sb.append((char) c);
                } else {
                    if (c > 255) {
                        sb.append(HEX_CHARS[(c >> 12) & 0xf]);
                        sb.append(HEX_CHARS[(c >> 8) & 0xf]);
                        sb.append('\\');
                    }
                    sb.append(HEX_CHARS[(c >> 4) & 0xf]);
                    sb.append(HEX_CHARS[c & 0xf]);
                }
            } else {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    public static boolean attributeEquals(final Attribute a1, final Attribute a2) {
        final boolean ordered = a1.isOrdered();
        if (a1.size() != a2.size() || a2.isOrdered() != ordered) {
            return false;
        }
        if (!a1.getID().equalsIgnoreCase(a2.getID())) {
            return false;
        }
        try {
            for (int q = a1.size() - 1; q >= 0; q--) {
                final Object value = a1.get(q);
                if (ordered) {
                    if (!value.equals(a2.get(q))) {
                        return false;
                    }
                } else {
                    if (!a2.contains(value)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static boolean attributesEquals(final Attributes a1, final Attributes a2) {
        if (a1.size() != a2.size()) {
            return false;
        }
        try {
            for (final NamingEnumeration<?> ne = a1.getAll(); ne.hasMore();) {
                final Attribute attr1 = (Attribute) ne.next();
                final Attribute attr2 = a2.get(attr1.getID());
                if (attr2 == null) {
                    return false;
                }
                if (!attributeEquals(attr1, attr2)) {
                    return false;
                }
            }
            return true;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static void createSubtree(final LdapContext ldap, final String name) {
        final Name rdn;
        try {
            rdn = ldap.getNameParser("").parse(name);
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
        for (int q = 1; q < rdn.size(); q++) {
            final String tree = rdn.getPrefix(q).toString();
            final String node = rdn.get(q - 1);
            final int idx = tree.indexOf('=');
            if (idx < 0) {
                throw new RuntimeException("Didn't find `=' in `" + tree + "'");
            }
            final BasicAttributes attributes = new BasicAttributes();
            final Attribute objectClass = new BasicAttribute("objectClass");
            objectClass.add("top");
            final String unit = node.substring(0, idx).trim().toLowerCase();
            final String unitname = node.substring(idx + 1);
            if (unitname.indexOf('\\') >= 0) {
                throw new RuntimeException("Backslash not supported: `" + name + "'");
            }
            if (unit.compareTo("c") == 0) {
                objectClass.add("country");
            } else if (unit.compareTo("o") == 0) {
                objectClass.add("organization");
            } else if (unit.compareTo("ou") == 0) {
                objectClass.add("organizationalUnit");
            } else if (unit.compareTo("dc") == 0) {
                objectClass.add("domain");
            } else {
                throw new RuntimeException("Unsupported unit `" + unit + "' in `" + name + "'");
            }
            attributes.put(unit, unitname);
            attributes.put(objectClass);
            try {
                ldap.createSubcontext(tree, attributes);
            } catch (final NameAlreadyBoundException e) {
                LOG.fine("Subtree `" + name + "' already exists");
            } catch (final NamingException e) {
                throw new UncheckedNamingException(e);
            }
        }
    }

    public static void store(final LdapContext ldap, final String rdn, final Attributes attributes, final boolean createSubtree) {
        try {
            ldap.rebind(rdn, null, attributes);
        } catch (final NameNotFoundException e) {
            if (createSubtree) {
                createSubtree(ldap, rdn);
                try {
                    ldap.rebind(rdn, null, attributes);
                } catch (final NamingException e1) {
                    throw new UncheckedNamingException(e);
                }
            } else {
                throw new UncheckedNamingException(e);
            }
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

    public static void store(final LdapContext ldap, final String rdn, final Attributes attributes) {
        store(ldap, rdn, attributes, false);
    }

    public static void remove(final LdapContext ldap, final String rdn) {
        try {
            ldap.unbind(rdn);
        } catch (final NameNotFoundException e) {
            LOG.fine("Attempt to remove non-existing entry `" + rdn + "'");
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

}
