-- SQL Server upgrade for an existing V-Mazz database.
-- The supplied Item entity already maps its product image to items.image_url.
IF COL_LENGTH('dbo.users', 'profile_image_url') IS NULL
BEGIN
    ALTER TABLE dbo.users ADD profile_image_url NVARCHAR(500) NULL;
END;
GO
