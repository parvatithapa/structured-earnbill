package com.sapienter.jbilling.rest;



/**
 * @author amey.pelapkar
 * @since 25th JUN 2021
 *
 */
public interface TestCrudOperation {

	/*
		// Login as admin : Cross Company, create <ENTITY> for another company  -- company2AdminApi	-- mordor;2		
		// Login as customer : create <ENTITY> for another company	-- company1Customer2Api		-- french-speaker;1		
		// Login as customer : create <ENTITY> in same company	-- company1Customer3Api	--	pendunsus1;1		
		// Login as admin(child company) : create <ENTITY> for parent company	-- parent1Company3AdminApi	--	admin;3
	*/
	public void testCreate();
	
	/*
		// Login as admin : Cross Company, get <ENTITY> for another company  -- company2AdminApi	-- mordor;2		
		// Login as customer : get <ENTITY> for another company	-- company1Customer2Api		-- french-speaker;1				
		// Login as customer : get <ENTITY> in same company	-- company1Customer3Api	--	pendunsus1;1				
		// Login as admin(child company) : get <ENTITY> for parent company	-- parent1Company3AdminApi	--	admin;3
	*/
	public void testRead();
	
	/*
		// Login as admin : Cross Company, update <ENTITY> for another company  -- company2AdminApi	-- mordor;2		
		// Login as customer : update <ENTITY> for another company	-- company1Customer2Api		-- french-speaker;1				
		// Login as customer : update <ENTITY> in same company	-- company1Customer3Api	--	pendunsus1;1				
		// Login as admin(child company) : update <ENTITY> for parent company	-- parent1Company3AdminApi	--	admin;3	
	*/
	public void testUpdate();
	
	/*
	 	// Login as admin : Cross Company, delete <ENTITY> for another company  -- company2AdminApi	-- mordor;2		
		// Login as customer : delete <ENTITY> for another company	-- company1Customer2Api		-- french-speaker;1				
		// Login as customer : delete <ENTITY> in same company	-- company1Customer3Api	--	pendunsus1;1				
		// Login as admin(child company) : delete <ENTITY> for parent company	-- parent1Company3AdminApi	--	admin;3	
	*/
	public void testDelete();	
}