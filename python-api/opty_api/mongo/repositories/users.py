"""
User repository for MongoDB operations.
"""

# --- IMPORTS ---
from datetime import datetime
from datetime import timezone
from opty_api.err.mongodb_unavailable_error import MongoUnavailableError
from opty_api.err.not_found_error import NotFoundError


# --- TYPES ---
from opty_api.schemas.user import User
from typing import Any
from typing import Dict
from typing import List
from typing import Optional


# --- CONSTANTS ---
PROJECTION = {'_id': 0}


# --- CODE ---
class UserRepository:
    """
    Repository for user-related MongoDB operations.
    Handles all database interactions for the users collection.
    """

    def __init__(self, client) -> None:
        """
        Initialize UserRepository with MongoDB client.

        :param client: MongoDB client instance
        """
        self.client = client


    @property
    def __collection(self):
        """
        Get users collection from MongoDB.
        """
        return self.client.get_collection('users')


    async def add_user(self, user: User) -> User:
        """
        Add a new user in MongoDB.

        :param user: User model instance

        :returns User: The created user

        :raises MongoUnavailableError: If insert fails
        """
        try:

            # get current UTC time
            now = datetime.now(timezone.utc)

            # set timestamps
            user['created_at'] = now
            user['updated_at'] = now

            # insert user into MongoDB
            await self.__collection.insert_one(user)

            # return the created user
            return user

        # error in create user: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to create user: {str(e)}') from e


    async def get_by_email(self, email: str, projection: Optional[Dict[str, int]] = PROJECTION) -> Optional[User]:  # pylint: disable=W0102
        """
        Find user by email.

        :param email: User email address
        :param projection: Fields to exclude in the result

        :returns: User if found, None otherwise

        :raises MongoUnavailableError: If query fails
        """
        try:

            # query MongoDB for user by email
            user_data = await self.__collection.find_one({'email': email, 'is_active': True}, projection)

            # user not found: return None
            if not user_data:
                return None

            # return User
            return user_data

        # error in find user: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to find user by email: {str(e)}') from e


    async def get_by_supabase_id(self,  # pylint: disable=W0102
                                 supabase_id: str,
                                 projection: Optional[Dict[str, int]] = PROJECTION) -> Optional[User]:
        """
        Find user by Supabase ID.

        :param supabase_id: Supabase user ID
        :param projection: Fields to exclude in the result

        :returns: User if found, None otherwise

        :raises MongoUnavailableError: If query fails
        """
        try:
            # query MongoDB for user by supabase_id
            user_data = await self.__collection.find_one({'supabase_id': supabase_id, 'is_active': True}, projection)

            # user not found: return None
            if not user_data:
                return None

            # return User
            return user_data

        # error in find user: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to find user by Supabase ID: {str(e)}') from e


    async def update_by_supabase_id(self, supabase_id: str, update_data: Dict[str, Any]) -> User:
        """
        Update user by Supabase ID.

        :param supabase_id: Supabase user ID
        :param update_data: Dictionary with fields to update

        :returns: Updated User if found, None otherwise

        :raises NotFoundError: If user not found
        :raises MongoUnavailableError: If update fails
        """
        try:

            # get user by supabase_id
            user = await self.get_by_supabase_id(supabase_id)

            # user not found: raise custom error
            if not user:
                raise NotFoundError(f'User with supabase_id {supabase_id} not found.')

            # update timestamp
            update_data['updated_at'] = datetime.now(timezone.utc)

            # Update in MongoDB
            await self.__collection.update_one(
                {'supabase_id': supabase_id},
                {'$set': update_data}
            )

            # get updated user
            updated_user = await self.get_by_supabase_id(supabase_id)

            # return updated user
            return updated_user

        # error in update user: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to update user by email: {str(e)}') from e


    async def update_by_email(self, email: str, update_data: Dict[str, Any]) -> User:
        """
        Update user by email.

        :param email: User email address
        :param update_data: Dictionary with fields to update

        :returns: Updated User if found, None otherwise

        :raises NotFoundError: If user not found
        :raises MongoUnavailableError: If update fails
        """
        try:

            # get user by email
            user = await self.get_by_email(email)

            # user not found: raise custom error
            if not user:
                raise NotFoundError(f'User with email {email} not found.')

            # update timestamp
            update_data['updated_at'] = datetime.now(timezone.utc)

            # Update in MongoDB
            await self.__collection.update_one(
                {'email': email},
                {'$set': update_data}
            )

            # get updated user
            updated_user = await self.get_by_email(email)

            # return updated user
            return updated_user

        # error in update user: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to update user by email: {str(e)}') from e


    async def delete_user(self, supabase_id: str) -> None:
        """
        Delete user.

        :param supabase_id: Supabase user ID

        :returns: True if deleted, False otherwise

        :raises NotFoundError: If user not found
        :raises MongoUnavailableError: If delete fails
        """
        try:

            # get user by supabase_id
            user = await self.get_by_supabase_id(supabase_id)

            # user not found: raise custom error
            if not user:
                raise NotFoundError(f'User with supabase_id {supabase_id} not found.')

            # soft delete user by setting is_active to False
            await self.__collection.update_one(
                {'supabase_id': supabase_id},
                {'$set': {
                    'is_active': False,
                    'updated_at': datetime.utcnow()
                }}
            )

        # error in delete user: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to delete user: {str(e)}') from e


    async def get_all(self,  # pylint: disable=W0102
                      skip: int = 0,
                      limit: int = 100,
                      projection: Optional[Dict[str, int]] = PROJECTION) -> List[User]:
        """
        Get all users with pagination.

        :param skip: Number of documents to skip
        :param limit: Maximum number of documents to return
        :param projection: Fields to exclude

        :returns: List of User
        """
        try:
            # get cursor with pagination
            cursor = self.__collection.find({'is_active': True}, projection).skip(skip).limit(limit)

            # get list of users
            users = await cursor.to_list(length=limit)

            # return users
            return users

        # error in list users: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to list users: {str(e)}') from e


    async def get_by_role(self,  # pylint: disable=W0102
                          role: str,
                          skip: int = 0,
                          limit: int = 100,
                          projection: Optional[Dict[str, int]] = PROJECTION) -> List[User]:
        """
        Get all users with specific role.

        :param role: User role (user or supervisor)
        :param skip: Number of documents to skip
        :param limit: Maximum number of documents to return
        :param projection: Fields to exclude

        :returns: List of User instances

        :raises MongoUnavailableError: If query fails
        """
        try:
            # get cursor with pagination
            cursor = self.__collection.find({'role': role, 'is_active': True}, projection).skip(skip).limit(limit)

            # get list of users
            users = await cursor.to_list(length=limit)

            # return users
            return users

        # error in find users by role: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to find users by role: {str(e)}') from e


    async def update_role(self, email: str, role: str) -> User:
        """
        Update user role.

        :param email: User email
        :param role: New role (user or supervisor)

        :returns: Updated User instance

        :raises MongoUnavailableError: If update fails
        :raises NotFoundError: If user not found
        """
        try:

            # update user role by email
            result = await self.update_by_email(email, {'role': role})

            # return updated user
            return result

        # error in update user role: raise custom error
        except Exception as e:
            raise MongoUnavailableError(f'Failed to update user role: {str(e)}') from e
