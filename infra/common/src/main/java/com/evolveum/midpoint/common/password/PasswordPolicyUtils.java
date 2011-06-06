/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.common.password;

import java.awt.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.string.StringPolicyException;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.result.OperationResult;
import com.evolveum.midpoint.util.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringPolicyType;

/**
 * 
 * @author mamut
 * 
 */
public class PasswordPolicyUtils {
	private static final transient Trace logger = TraceManager.getTrace(PasswordPolicyUtils.class);
/**
 * 
 * @param pp
 * @return
 * @throws PasswordPolicyException
 */
	public static PasswordPolicyType initialize(PasswordPolicyType pp) throws PasswordPolicyException {
		if (null == pp) {
			throw new PasswordPolicyException(
					new OperationResult("Password Policy Initialize ", OperationResultStatus.FATAL_ERROR,
							"PPU-001", "Provided password policcy cannot be null."));
		}

		if (null == pp.getStringPolicy()) {
			StringPolicyType sp = new StringPolicyType();
			try {
				pp.setStringPolicy(com.evolveum.midpoint.common.string.Utils.initialize(sp));
			} catch (StringPolicyException spe) {
				throw new PasswordPolicyException(spe.getResult());
			}
		}

		if (null == pp.getLifetime()) {
			PasswordLifeTimeType lt = new PasswordLifeTimeType();
			lt.setExpiration(-1);
			lt.setWarnBeforeExpiration(0);
			lt.setLockAfterExpiration(0);
			lt.setMinPasswordAge(0);
			lt.setPasswordHistoryLength(0);
		}
		return pp;
	}

	public static OperationResult validatePassword (String password, PasswordPolicyType pp) {
		return null;
	}
	
	/**
	 * Generate password on provided password policy
	 * 
	 * @param pp
	 *            password policy
	 * @return Generated password string
	 * @throws PasswordPolicyException
	 */
	public static String generatePassword(PasswordPolicyType pp) throws PasswordPolicyException {

		// If no police defined use default values
		if (null == pp) {
			throw new IllegalArgumentException("Provided password policy is NULL");
		}
		// Add missing and default parts
		pp = initialize(pp);

		//Get global borders
		int minSize = pp.getStringPolicy().getLimitations().getMinLength();
		int maxSize = pp.getStringPolicy().getLimitations().getMaxLength();
		int uniqChars = pp.getStringPolicy().getLimitations().getMinUniqueChars(); //TODO

		if (uniqChars > minSize) {
			minSize = uniqChars;
		}

		if (-1 != maxSize && minSize > maxSize) {
			throw new PasswordPolicyException(new OperationResult("PPG", OperationResultStatus.FATAL_ERROR,
					"PPG-001", "Minimal size (" + minSize + ") cannot be bigger then maximal size ("
							+ maxSize + ") in password policy:"
							+ (null == pp.getName() ? "Unamed policy" : pp.getName())));
		}

		// find limitation which need to be run as first
		ArrayList<StringLimitType> limits = shuffleLimitations(pp.getStringPolicy().getLimitations());

		StringBuilder password = new StringBuilder();
		Random r = new Random(System.currentTimeMillis());
		
		for (StringLimitType l : limits) {
			normalizeLimit(l);
			logger.debug("Processing limit: " + l.getDescription());
			
			if (isAlreadyMeetThis(pp, l, password.toString())) {
				// next iteration
				continue;
			} else {
				//Generate character based on policy
				ArrayList<String> validChars = extractValidChars(pp, l);
				password.append(validChars.get(r.nextInt(validChars.size())));
			}
		}

		// Check if maximal size not exceeded
		if (maxSize != -1 && maxSize < password.length()) {
			String msg = "Maximal size (" + maxSize
					+ ") of password was exceeded because generated password size was (" + password.length()
					+ ") based on password policy:" + (null == pp.getName() ? "Unamed policy" : pp.getName())
					+ ".To fix it please make policy less restrictive";

			logger.error(msg);
			throw new PasswordPolicyException(new OperationResult("PPG", OperationResultStatus.FATAL_ERROR,
					"PPG-002", msg));
		}

		// Check if minimal criteria was meet
		if (minSize <= password.length()) {
			return password.toString();
		}
		// Look like minimal criteria not meet.

		return null;
	}

	/**
	 * Normalize and add default values
	 * @param l
	 */
	private static void normalizeLimit(StringLimitType l) {
		if (null == l) {
			throw new IllegalArgumentException();
		}
		if ( null == l.getMaxOccurs()) {
			l.setMaxOccurs(-1);
		}
		
		if ( null == l.getMinOccurs()) {
			l.setMinOccurs(0);
		}
	}

	

	/*
	 * Check if any character not meet policy
	 */
	private static boolean isAlreadyMeetThis(PasswordPolicyType pp, StringLimitType l, String password)
			throws PasswordPolicyException {
		// Get Valid chars for this policy
		ArrayList<String> validChars = extractValidChars(pp, l);

		// count how many characters from password is there
		int counter = 0;
		if (password.length() != 0) {
			for (String letter : tokenizeString(password)) {
				if (validChars.contains(letter)) {
					counter++;
				}
			}
		}
		
		// no character required character there
		if (counter == 0) {
			return false;
			// if number of characters not meet minimal criteria
		} else if (counter < l.getMinOccurs()) {
			return false;
			// if number of characters not exceed maximum
		} else if (counter > l.getMaxOccurs() && l.getMaxOccurs() != -1) {
			throw new PasswordPolicyException(new OperationResult("Limitation check",
					OperationResultStatus.FATAL_ERROR, "PP-003",
					"Provided password exceed maximal length of charaters for this limitation ("
							+ l.getDescription() + ") in password policy:" + pp.getName()));

			// If required characters is on first position
		}
		if (l.isMustBeFirst() && !validChars.contains(password.subSequence(0, 0))) {
			throw new PasswordPolicyException(new OperationResult("Limitation check",
					OperationResultStatus.FATAL_ERROR, "PP-003",
					"Provided password exceed maximal length of charaters for this limitation ("
							+ l.getDescription() + ") in password policy:" + pp.getName()));
		}
		logger.debug("Criteria for password was met. -> " + l.getDescription());
		return true;
	}

	/**
	 * Extract validchars for specified limitation from policy
	 * 
	 * @param pp
	 *            - password policy
	 * @param l
	 *            - limitation
	 * @return Array of unique valid characters
	 */

	private static ArrayList<String> extractValidChars(PasswordPolicyType pp, StringLimitType l)
			throws IllegalArgumentException {
		if (null == pp || null == l) {
			throw new IllegalArgumentException("Provided password policy or limitation is null.");
		}
		ArrayList<String> validChars = new ArrayList<String>();
		// Is any character class defined in limitation?
		if (null != l.getCharacterClass()) {
			// Is it referenced or included
			if (null != l.getCharacterClass().getRef()) {
				// It's included
				logger.debug("Characters are referenced to : "
						+ l.getCharacterClass().getRef().getLocalPart());
				validChars = tokenizeCharacterClass(pp.getStringPolicy().getCharacterClass(), l
						.getCharacterClass().getRef());
			} else {
				// it's included
				logger.debug("Characters are included :" + l.getCharacterClass().getValue());
				validChars = tokenizeCharacterClass(l.getCharacterClass(),null);
			}
		} else {
			// use whole class
			logger.debug("Cahracters are not included and not referenced use all from policy.");
			validChars = tokenizeCharacterClass(pp.getStringPolicy().getCharacterClass(),null);
		}
		if (validChars.size() == 0) {
			logger.error("No valid chars found in " + pp.getName() + "for " + l.getDescription());
		}
		logger.debug("Valid chars are:" + listToString(validChars));
		return validChars;
	}

	/*
	 * Parse out limitations and add them shuffled to list
	 */
	private static ArrayList<StringLimitType> shuffleLimitations(LimitationsType lims) {
		if (null == lims || lims.getLimit().isEmpty()) {
			return new ArrayList<StringLimitType>();
		}
		ArrayList<StringLimitType> partA = new ArrayList<StringLimitType>();
		ArrayList<StringLimitType> partB = new ArrayList<StringLimitType>();

		// Split to which can be first and which not
		
		for (StringLimitType l : lims.getLimit()) {
			if (l.isMustBeFirst() == true) {
				partA.add(l);
			} else {
				partB.add(l);
			}
			//If there is more characters of this type required just multiply limits
			if (l.getMinOccurs() > 1) {
				for ( int i = 1 ; i < l.getMinOccurs() ; i++ ) {
					partB.add(l);
				}
			}
		}

		// Shuffle both
		Collections.shuffle(partA, new Random(System.currentTimeMillis()));
		Collections.shuffle(partB, new Random(System.currentTimeMillis() - 4555));

		// Add limitations which cannot be first to end
		partA.addAll(partB);
		return partA;
	}

	
	/*
	 * Prepare usable list of strings for generator
	 */
	private static ArrayList<String> tokenizeCharacterClass(CharacterClassType cc, QName ref) {
		ArrayList<String> l = new ArrayList<String>();
		if ( null == cc ) {
			throw new IllegalArgumentException("Character class cannot be null");
		}
		
		if (null != cc.getValue() && ( null == ref || ref.equals(cc.getName()) )) {
			String a[] = cc.getValue().split("");
			// Add all to list
			for (int i = 1; i < a.length; i++) {
				l.add(a[i]);
			}
		} else if (null != cc.getCharacterClass() && !cc.getCharacterClass().isEmpty()) {
			// Process all sub lists
			for (CharacterClassType subClass : cc.getCharacterClass()) {
				// If we found requested name or no name defined
				if ( null == ref || ref.equals(cc.getName()) ) {
					l.addAll(tokenizeCharacterClass(subClass, null));
				} else {
					l.addAll(tokenizeCharacterClass(subClass, ref));
				}
			}
		}
		// Remove duplicity in return;
		return new ArrayList<String>(new HashSet<String>(l));
	}

	private static ArrayList<String> tokenizeString(String in) {
		ArrayList<String> l = new ArrayList<String>();
		String a[] = in.split("");
		// Add all to list
		for (int i = 0; i < a.length; i++) {
			l.add(a[i]);
		}
		return l;
	}
	
	private static String listToString ( ArrayList<String> l){
		 if ( l.size() == 0 ) {
			 return "[ ]";
		 }
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String i: l) {
			sb.append(i);
			sb.append(" ,");
		}
		sb.append("]");
		return sb.toString();
	}
}
