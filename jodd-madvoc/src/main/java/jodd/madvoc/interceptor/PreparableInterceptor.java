// Copyright (c) 2003-2014, Jodd Team (jodd.org). All Rights Reserved.

package jodd.madvoc.interceptor;

import jodd.madvoc.ActionRequest;
import jodd.madvoc.ScopeData;
import jodd.madvoc.ScopeType;
import jodd.madvoc.component.MadvocConfig;
import jodd.madvoc.component.ScopeDataResolver;
import jodd.madvoc.injector.RequestScopeInjector;
import jodd.madvoc.meta.In;
import jodd.util.StringUtil;

/**
 * Prepares action by calling <code>prepare()</code> before action method invocation.
 * <p>
 * A typical use of this is to run some logic to load an object from the database, so that when parameters are set
 * they can be set on this object. For example, suppose you have a User object with two properties: id and name.
 * Provided that the params interceptor is called twice (once before and once after this interceptor), you can load the
 * User object using the id property, and then when the second params interceptor is called the parameter user.name will
 * be set, as desired, on the actual object loaded from the database.
 * <p>
 * Optionally, preparable interceptor injects request parameters that ends with ".id" or "Id"
 * so you can immediately  load objects from storage before preparing.
 */
public class PreparableInterceptor extends BaseActionInterceptor {

	protected static final String[] ATTR_NAME_ID_SUFFIXES = new String[] {".id", "Id"};

	@In(scope = ScopeType.CONTEXT)
	protected MadvocConfig madvocConfig;

	@In(scope = ScopeType.CONTEXT)
	protected ScopeDataResolver scopeDataResolver;

	protected RequestScopeInjector requestInjector;

	@Override
	public void init() {
		requestInjector = new RequestScopeInjector(madvocConfig, scopeDataResolver) {
			@Override
			protected String getMatchedPropertyName(ScopeData.In in, String attrName) {
				if (StringUtil.endsWithOne(attrName, ATTR_NAME_ID_SUFFIXES) == -1) {
					// no match
					return null;
				}
				return super.getMatchedPropertyName(in, attrName);
			}
		};
		requestInjector.setInjectAttributes(false);
	}


	// ---------------------------------------------------------------- flags

	protected boolean isInjectIdsFromRequestEnabled = false;

	public boolean isInjectIdsFromRequestEnabled() {
		return isInjectIdsFromRequestEnabled;
	}

	public void setInjectIdsFromRequestEnabled(boolean isInjectIdsFromRequestEnabled) {
		this.isInjectIdsFromRequestEnabled = isInjectIdsFromRequestEnabled;
	}

	// ---------------------------------------------------------------- intercept

	/**
	 * {@inheritDoc}
	 */
	public Object intercept(ActionRequest actionRequest) throws Exception {
		Object action = actionRequest.getAction();
		if (action instanceof Preparable) {

			if (isInjectIdsFromRequestEnabled) {
				injectIdsFromRequest(actionRequest);
			}

			((Preparable) action).prepare();
		}
		return actionRequest.invoke();
	}

	/**
	 * Injects IDs from request. Invoked before action request is invoked.
	 */
	protected void injectIdsFromRequest(ActionRequest actionRequest) {
		requestInjector.inject(actionRequest);
	}
}
