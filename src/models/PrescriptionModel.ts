import AWS from 'aws-sdk';
import { v4 as uuidv4 } from 'uuid';
import { Prescription, PrescriptionSchedule, DynamoDBItem } from '../types';

export class PrescriptionModel {
  private dynamodb: AWS.DynamoDB.DocumentClient;
  private tableName: string;
  private scheduleTableName: string;

  constructor() {
    this.dynamodb = new AWS.DynamoDB.DocumentClient();
    this.tableName = process.env.PRESCRIPTIONS_TABLE || 'prescriptions';
    this.scheduleTableName = process.env.PRESCRIPTION_SCHEDULES_TABLE || 'prescription-schedules';
  }

  async createPrescription(patientId: string, prescription: Omit<Prescription, 'prescriptionId' | 'patientId' | 'createdAt' | 'updatedAt'>): Promise<Prescription> {
    const prescriptionId = uuidv4();
    const now = new Date().toISOString();
    
    const newPrescription: Prescription & DynamoDBItem = {
      PK: `PATIENT#${patientId}`,
      SK: `PRESCRIPTION#${prescriptionId}`,
      GSI1PK: `PRESCRIPTION#${prescriptionId}`,
      GSI1SK: 'METADATA',
      GSI2PK: `STATUS#${prescription.status}`,
      GSI2SK: now,
      prescriptionId,
      patientId,
      ...prescription,
      createdAt: now,
      updatedAt: now
    };

    await this.dynamodb.put({
      TableName: this.tableName,
      Item: newPrescription
    }).promise();

    const { PK, SK, GSI1PK, GSI1SK, GSI2PK, GSI2SK, ...prescriptionData } = newPrescription;
    return prescriptionData;
  }

  async getPrescriptionsByPatient(patientId: string): Promise<Prescription[]> {
    const params = {
      TableName: this.tableName,
      KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
      ExpressionAttributeValues: {
        ':pk': `PATIENT#${patientId}`,
        ':sk': 'PRESCRIPTION#'
      }
    };

    const result = await this.dynamodb.query(params).promise();
    return result.Items?.map(item => {
      const { PK, SK, GSI1PK, GSI1SK, GSI2PK, GSI2SK, ...prescription } = item;
      return prescription as Prescription;
    }) || [];
  }

  async getPrescriptionById(prescriptionId: string): Promise<Prescription | null> {
    const params = {
      TableName: this.tableName,
      IndexName: 'GSI1',
      KeyConditionExpression: 'GSI1PK = :gsi1pk AND GSI1SK = :gsi1sk',
      ExpressionAttributeValues: {
        ':gsi1pk': `PRESCRIPTION#${prescriptionId}`,
        ':gsi1sk': 'METADATA'
      }
    };

    const result = await this.dynamodb.query(params).promise();
    if (!result.Items || result.Items.length === 0) {
      return null;
    }

    const item = result.Items[0];
    const { PK, SK, GSI1PK, GSI1SK, GSI2PK, GSI2SK, ...prescription } = item;
    return prescription as Prescription;
  }

  async updatePrescription(prescriptionId: string, updates: Partial<Prescription>): Promise<Prescription | null> {
    const existing = await this.getPrescriptionById(prescriptionId);
    if (!existing) {
      return null;
    }

    const now = new Date().toISOString();
    const updateExpression = [];
    const expressionAttributeNames: any = {};
    const expressionAttributeValues: any = {};

    Object.entries(updates).forEach(([key, value]) => {
      if (key !== 'prescriptionId' && key !== 'patientId' && key !== 'createdAt') {
        updateExpression.push(`#${key} = :${key}`);
        expressionAttributeNames[`#${key}`] = key;
        expressionAttributeValues[`:${key}`] = value;
      }
    });

    updateExpression.push('#updatedAt = :updatedAt');
    expressionAttributeNames['#updatedAt'] = 'updatedAt';
    expressionAttributeValues[':updatedAt'] = now;

    if (updates.status) {
      updateExpression.push('GSI2PK = :gsi2pk');
      expressionAttributeValues[':gsi2pk'] = `STATUS#${updates.status}`;
    }

    const params = {
      TableName: this.tableName,
      Key: {
        PK: `PATIENT#${existing.patientId}`,
        SK: `PRESCRIPTION#${prescriptionId}`
      },
      UpdateExpression: `SET ${updateExpression.join(', ')}`,
      ExpressionAttributeNames: expressionAttributeNames,
      ExpressionAttributeValues: expressionAttributeValues,
      ReturnValues: 'ALL_NEW' as const
    };

    const result = await this.dynamodb.update(params).promise();
    if (!result.Attributes) {
      return null;
    }

    const { PK, SK, GSI1PK, GSI1SK, GSI2PK, GSI2SK, ...prescription } = result.Attributes;
    return prescription as Prescription;
  }

  async deletePrescription(prescriptionId: string): Promise<boolean> {
    const existing = await this.getPrescriptionById(prescriptionId);
    if (!existing) {
      return false;
    }

    await this.dynamodb.delete({
      TableName: this.tableName,
      Key: {
        PK: `PATIENT#${existing.patientId}`,
        SK: `PRESCRIPTION#${prescriptionId}`
      }
    }).promise();

    return true;
  }

  async getActivePrescriptions(): Promise<Prescription[]> {
    const params = {
      TableName: this.tableName,
      IndexName: 'GSI2',
      KeyConditionExpression: 'GSI2PK = :gsi2pk',
      ExpressionAttributeValues: {
        ':gsi2pk': 'STATUS#ACTIVE'
      }
    };

    const result = await this.dynamodb.query(params).promise();
    return result.Items?.map(item => {
      const { PK, SK, GSI1PK, GSI1SK, GSI2PK, GSI2SK, ...prescription } = item;
      return prescription as Prescription;
    }) || [];
  }
}